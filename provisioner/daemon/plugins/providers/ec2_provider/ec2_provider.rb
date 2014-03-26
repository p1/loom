#!/usr/bin/env ruby
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

gem 'knife-ec2', '= 0.8.0'

require 'json'
require_relative 'chef/knife/loom_ec2_server_create'
require_relative 'chef/knife/loom_ec2_server_confirm'
require 'chef/knife/ec2_server_delete'

class Ec2Provider < Provider

  def create(inputmap)
    flavor = inputmap['flavor']
    image = inputmap['image']
    hostname = inputmap['hostname']

    begin
      knife_instance = Chef::Knife::LoomEc2ServerCreate.new
      knife_instance.configure_chef

      Chef::Config[:knife][:flavor] = flavor
      Chef::Config[:knife][:image] = image
      knife_instance.config[:chef_node_name] = hostname

      @task["config"]["provider"]["provisioner"]["auth"].each do |k,v|
        Chef::Config[:knife][k.to_sym] = v
      end

      # invoke knife
      log.debug "Invoking server create"
      kniferesult = knife_instance.run
      @result['result']['providerid'] = kniferesult['providerid']
      @result['result']['ssh-auth']['user'] = "root"
      if kniferesult.has_key? 'rootpassword'
        @result['result']['ssh-auth']['password'] = kniferesult['rootpassword']
      else
        @result['result']['ssh-auth']['identityfile'] = Chef::Config[:knife][:identity_file] || "/opt/loom/provisioner/daemon/plugins/providers/ec2_provider/id_rsa"
      end
      @result['status'] = kniferesult['status']
    rescue Exception => e
      log.error('Unexpected Error Occured in Ec2Provider.create:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in Ec2Provider.create: #{e.inspect}"
    else
      log.debug "Create finished successfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  def confirm(inputmap)
    providerid = inputmap['providerid']

    begin
      knife_instance = Chef::Knife::LoomEc2ServerConfirm.new
      knife_instance.configure_chef
      knife_instance.name_args.push(providerid)

      @task["config"]["provider"]["provisioner"]["auth"].each do |k,v|
        Chef::Config[:knife][k.to_sym] = v
      end

      # invoke knife
      log.debug "Invoking server confirm"
      kniferesult = knife_instance.run

      @result['result']['ipaddress'] = kniferesult['ipaddress']
      raise "non-zero exit code: #{kniferesult['status']} from knife ec2" unless kniferesult['status'] == 0

      # additional checks, we want to make sure we can login and verify external dns
      log.debug "Confirming ssh is up"
      knife_instance.tcp_test_ssh(kniferesult['ipaddress'], knife_instance.locate_config_value(:ssh_port)) { sleep 5 }
      set_credentials(@task['config']['ssh-auth'])

      Net::SSH.start(kniferesult['ipaddress'], @task['config']['ssh-auth']['user'], @credentials) do |ssh|
        # validate connectivity
        log.debug "Validating dns resolution/connectivity"
        output = ssh_exec!(ssh, "ping -c1 www.opscode.com")
      end

      @result['status'] = 0

    rescue Net::SSH::AuthenticationFailed => e
      log.error("SSH Authentication failure for #{providerid}/#{kniferesult['ipaddress']}")
      @result['stderr'] = "SSH Authentication failure for #{providerid}/#{kniferesult['ipaddress']}: #{e.inspect}"
    rescue Exception => e
      log.error('Unexpected Error Occured in Ec2Provider.confirm:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in Ec2Provider.confirm: #{e.inspect}"
    else
      log.debug "Confirm finished successfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  def delete(inputmap)
    providerid = inputmap['providerid']

    begin
      knife_instance = Chef::Knife::Ec2ServerDelete.new
      knife_instance.configure_chef
      knife_instance.name_args.push(providerid)
      knife_instance.config[:yes] = true

      @task["config"]["provider"]["provisioner"]["auth"].each do |k,v|
        Chef::Config[:knife][k.to_sym] = v
      end

      # invoke knife
      log.debug "Invoking server delete"
      kniferesult = knife_instance.run

      @result['status'] = 0

    rescue Exception => e
      log.error('Unexpected Error Occured in Ec2Provider.delete:' + e.inspect)
      @result['stderr'] = "Unexpected Error Occured in Ec2Provider.delete: #{e.inspect}"
    else
      log.debug "Delete finished sucessfully: #{@result}"
    ensure
      @result['status'] = 1 if @result['status'].nil? || (@result['status'].is_a?(Hash) && @result['status'].empty?)
    end
  end

  # used by ssh validation in confirm stage
  def set_credentials(sshauth)
    @credentials = Hash.new
    @credentials[:paranoid] = false
    sshauth.each do |k, v|
      if (k =~ /password/)
        @credentials[:password] = v
      elsif (k =~ /identityfile/)
        @credentials[:keys] = [ v ]
      end
    end
  end

end

