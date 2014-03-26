#
# Copyright 2012-2014, Continuuity, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
require 'chef/knife/ec2_base'
require 'chef/knife/ec2_server_create'

class Chef
  class Knife
    class LoomEc2ServerConfirm < Ec2ServerCreate

      Chef::Knife::Ec2ServerCreate.load_deps

      # support options provided by ec2 base module
      include Knife::Ec2Base

      # quick workaround for class level instance vars
      self.options.merge!(Ec2ServerCreate.options)

      # load deps
      deps do
        require 'fog'
        require 'readline'
        require 'chef/json_compat'
        require 'chef/knife/bootstrap'
        require 'ipaddr'
        Chef::Knife::Bootstrap.load_deps
      end

      banner 'knife loom ec2 server confirm (options) SERVER-ID'

      def run
        $stdout.sync = true

        unless name_args.size === 1
          show_usage
          exit 1
        end

        requested_elastic_ip = config[:associate_eip] if config[:associate_eip]

        # For VPC EIP assignment we need the allocation ID so fetch full EIP details
        elastic_ip = connection.addresses.detect{|addr| addr if addr.public_ip == requested_elastic_ip}

        id = name_args.first
        puts "fetching server for id: #{id}"
        @server = connection.servers.create(create_server_def)

        hashed_tags={}
        tags.map{ |t| key,val=t.split('='); hashed_tags[key]=val} unless tags.nil?

        # Always set the Name tag
        unless hashed_tags.keys.include? "Name"
          hashed_tags["Name"] = locate_config_value(:chef_node_name) || @server.id
        end

        printed_tags = hashed_tags.map{ |tag, val| "#{tag}: #{val}" }.join(", ")

        Chef::Log.debug("Server state: #{server.state}")

        if (server.state == 'ERROR')
          ui.error("Server #{id} has failed")
          exit 1
        end

        # start copy from ec2 server create

        # wait for it to be ready to do stuff
        print "\n#{ui.color("Waiting for instance", :magenta)}"

        server.wait_for(1200) { print "."; ready? }

        puts("\n")

        # occasionally 'ready?' isn't, so retry a couple times if needed.
        tries = 6
        begin
          create_tags(hashed_tags) unless hashed_tags.empty?
          associate_eip(elastic_ip) if config[:associate_eip]
        rescue Fog::Compute::AWS::NotFound, Fog::Errors::Error
          raise if (tries -= 1) <= 0
          ui.warn("server not ready, retrying tag application (retries left: #{tries})")
          sleep 5
          retry
        end

        if vpc_mode?
          msg_pair("Subnet ID", @server.subnet_id)
          msg_pair("Tenancy", @server.tenancy)
          if config[:associate_public_ip]
            msg_pair("Public DNS Name", @server.dns_name)
          end
          if elastic_ip
            msg_pair("Public IP Address", @server.public_ip_address)
          end
        else
          msg_pair("Public DNS Name", @server.dns_name)
          msg_pair("Public IP Address", @server.public_ip_address)
          msg_pair("Private DNS Name", @server.private_dns_name)
        end
        msg_pair("Private IP Address", @server.private_ip_address)

        print "\n#{ui.color("Waiting for sshd", :magenta)}"
        wait_for_sshd(ssh_connect_host)

        puts "\n"
        msg_pair("Instance ID", @server.id)
        msg_pair("Flavor", @server.flavor_id)
        msg_pair("Image", @server.image_id)
        msg_pair("Placement Group", @server.placement_group) unless @server.placement_group.nil?
        msg_pair("Region", connection.instance_variable_get(:@region))
        msg_pair("Availability Zone", @server.availability_zone)

        # If we don't specify a security group or security group id, Fog will
        # pick the appropriate default one. In case of a VPC we don't know the
        # default security group id at this point unless we look it up, hence
        # 'default' is printed if no id was specified.
        printed_security_groups = "default"
        printed_security_groups = @server.groups.join(", ") if @server.groups
        msg_pair("Security Groups", printed_security_groups) unless vpc_mode? or (@server.groups.nil? and @server.security_group_ids)

        printed_security_group_ids = "default"
        printed_security_group_ids = @server.security_group_ids.join(", ") if @server.security_group_ids

        msg_pair("Security Group Ids", printed_security_group_ids) if vpc_mode? or @server.security_group_ids
        msg_pair("IAM Profile", locate_config_value(:iam_instance_profile)) if locate_config_value(:iam_instance_profile)
        msg_pair("Tags", printed_tags)
        msg_pair("SSH Key", @server.key_name)
        msg_pair("Root Device Type", @server.root_device_type)

        if @server.root_device_type == "ebs"
          device_map = @server.block_device_mapping.first
          msg_pair("Root Volume ID", device_map['volumeId'])
          msg_pair("Root Device Name", device_map['deviceName'])
          msg_pair("Root Device Delete on Terminate", device_map['deleteOnTermination'])

          if config[:ebs_size]
            if ami.block_device_mapping.first['volumeSize'].to_i < config[:ebs_size].to_i
              volume_too_large_warning = "#{config[:ebs_size]}GB " +
                          "EBS volume size is larger than size set in AMI of " +
                          "#{ami.block_device_mapping.first['volumeSize']}GB.\n" +
                          "Use file system tools to make use of the increased volume size."
              msg_pair("Warning", volume_too_large_warning, :yellow)
            end
          end
        end
        if config[:ebs_optimized]
          msg_pair("EBS is Optimized", @server.ebs_optimized.to_s)
        end

        if vpc_mode?
          msg_pair("Subnet ID", @server.subnet_id)
          msg_pair("Tenancy", @server.tenancy)
          if config[:associate_public_ip]
            msg_pair("Public DNS Name", @server.dns_name)
          end
        else
          msg_pair("Public DNS Name", @server.dns_name)
          msg_pair("Public IP Address", @server.public_ip_address)
          msg_pair("Private DNS Name", @server.private_dns_name)
        end
        msg_pair("Private IP Address", @server.private_ip_address)
        msg_pair("Environment", config[:environment] || '_default')
        msg_pair("Run List", (config[:run_list] || []).join(', '))
        msg_pair("JSON Attributes",config[:json_attributes]) unless !config[:json_attributes] || config[:json_attributes].empty?

        # private_ip_address is always there, but override it with a public_ip_address, if available
        bootstrap_ip_address = @server.private_ip_address if @server.private_ip_address
        bootstrap_ip_address = @server.public_ip_address if @server.public_ip_address

        Chef::Log.debug("Bootstrap IP Address #{bootstrap_ip_address}")
        if bootstrap_ip_address.nil?
          ui.error("No IP address available for bootstrapping.")
          exit 1
        end

        puts ui.color("Bootstrap IP Address #{bootstrap_ip_address}", :cyan)

        return { "status" => 0, "ipaddress" => bootstrap_ip_address }

      end # run

    end
  end
end

