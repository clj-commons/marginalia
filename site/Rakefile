# -*- ruby -*-
#
# This is a sample Rakefile to which you can add tasks to manage your website. For example, users
# may use this file for specifying an upload task for their website (copying the output to a server
# via rsync, ftp, scp, ...).
#
# It also provides some tasks out of the box, for example, rendering the website, clobbering the
# generated files, an auto render task,...
#

require 'webgen/webgentask'
require 'webgen/website'

task :default => :webgen

webgen_config = lambda do |config|
  # you can set configuration options here
end

Webgen::WebgenTask.new do |website|
  website.clobber_outdir = true
  website.config_block = webgen_config
end

desc "Show outdated translations"
task :outdated do
  puts "Listing outdated translations"
  puts
  puts "(Note: Information is taken from the last webgen run. To get the"
  puts "       useful information, run webgen once before this task!)"
  puts

  website = Webgen::Website.new(Dir.pwd, Webgen::Logger.new($stdout), &webgen_config)
  website.execute_in_env do
    website.init
    website.tree.node_access[:acn].each do |acn, versions|
      main = versions.find {|v| v.lang == website.config['website.lang']}
      next unless main
      outdated = versions.select do |v|
         main != v && main['modified_at'] > v['modified_at']
      end.map {|v| v.lang}.join(', ')
      puts "ACN #{acn}: #{outdated}" if outdated.length > 0
    end
  end
end

desc "Render the website automatically on changes"
task :auto_webgen do
  puts 'Starting auto-render mode'
  time = Time.now
  abort = false
  old_paths = []
  Signal.trap('INT') {abort = true}

  while !abort
    # you may need to adjust the glob so that all your sources are included
    paths = Dir['src/**/*'].sort
    if old_paths != paths || paths.any? {|p| File.mtime(p) > time}
      begin
        Rake::Task['webgen'].execute({})
      rescue Webgen::Error => e
        puts e.message
      end
    end
    time = Time.now
    old_paths = paths
    sleep 2
  end
end
