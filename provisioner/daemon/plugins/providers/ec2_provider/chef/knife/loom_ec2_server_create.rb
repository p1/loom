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
    class LoomEc2ServerCreate < Ec2ServerCreate

      Chef::Knife::Ec2ServerCreate.load_deps

      # support options provided by ec2 base module
      include Knife::Ec2Base

      # quick workaround for class level instance vars 
      self.options.merge!(Ec2ServerCreate.options)

      # load dep 
      deps do
        require 'fog'
        require 'readline'
        require 'chef/json_compat'
        require 'chef/knife/bootstrap'
        Chef::Knife::Bootstrap.load_deps
      end

      banner 'knife loom ec2 server create (options)'

      def run

        $stdout.sync = true

        # Paste from knife-ec2 goes here
        validate!

        requested_elastic_ip = config[:associate_eip] if config[:associate_eip]

        # For VPC EIP assignment we need the allocation ID so fetch full EIP details
        elastic_ip = connection.addresses.detect{|addr| addr if addr.public_ip == requested_elastic_ip}

        @server = connection.servers.create(create_server_def)

        hashed_tags={}
        tags.map{ |t| key,val=t.split('='); hashed_tags[key]=val} unless tags.nil?

        # Always set the Name tag
        unless hashed_tags.keys.include? "Name"
          hashed_tags["Name"] = locate_config_value(:chef_node_name) || @server.id
        end

        printed_tags = hashed_tags.map{ |tag, val| "#{tag}: #{val}" }.join(", ")

        msg_pair("Instance ID", @server.id)
        msg_pair("Flavor", @server.flavor_id)
        msg_pair("Image", @server.image_id)
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

        msg_pair("IAM Profile", locate_config_value(:iam_instance_profile))

        msg_pair("Tags", printed_tags)
        msg_pair("SSH Key", @server.key_name)

        puts "SERVERID: #{server.id.to_s}"
        if (server.password && !server.key_name)
          return { "status" => 0, "providerid" => server.id.to_s, "rootpassword" => server.password }
        else
          return { "status" => 0, "providerid" => server.id.to_s }
        end
      end
    end
  end
end

