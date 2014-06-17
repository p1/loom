#!/usr/bin/env ruby

require 'thin'
require 'sinatra/base'
require 'json'

require_relative 'tenantmanager'
require_relative 'provisioner'

module Loom
  class ProvisionerApi < Sinatra::Base

    attr_accessor :provisioner

    def initialize()
      super()
      @provisioner = Loom::Provisioner.new
    end

    get '/hi' do
      "Hello World!"
    end

    get '/hello/:name' do
      # matches "GET /hello/foo" and "GET /hello/bar"
      # params[:name] is 'foo' or 'bar'
      "Hello #{params[:name]}!"
    end

    get '/status' do
      puts "total ruby threads: #{Thread.list.size}"
      @provisioner.status
      body "OK"
    end

    get '/heartbeat' do
      @provisioner.heartbeat.to_json
    end

    post "/v1/tenants" do
      puts "adding tenant"
      data = JSON.parse request.body.read
      id = data['id']
      workers = data['workers']
      modules = data['modules'] || nil
      plugins = data['plugins'] || nil

      ts = TenantSpec.new(id, workers, modules, plugins)
      tm = TenantManager.new(ts)

      @provisioner.add_tenant(tm)

      data['status'] = 0
      body data.to_json
    end

    put "/v1/tenants/:t_id" do
      puts "adding/updating tennant id: #{params[:t_id]}"
      data = JSON.parse request.body.read 
      workers = data['workers'] || 3 # TO DO: replace default with constant
      puts "requesting workers: #{workers}"
      modules = data['modules'] || nil
      puts "requesting modules: #{modules}"
      plugins = data['plugins'] || nil
      puts "requesting plugins: #{plugins}"

      ts = TenantSpec.new(params[:t_id], workers, modules, plugins)
      tm = TenantManager.new(ts)

      @provisioner.add_tenant(tm)
#      provisioner = provisioner.getinstance
#      provisioner.add_tenant(tm)

#      tm.spawn

      data['status'] = 0

      #data = JSON.parse(params[:data])
      body data.to_json
    end

    delete "/v1/tenants/:t_id" do
      @provisioner.delete_tenant(params[:t_id])
    end

    # replace with start script
    run! if app_file == $0
  end
end
