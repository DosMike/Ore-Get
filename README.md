# Ore-Get
A apt-get inspired Plugin management tool

### How it works
This plugin connects to the Ore API in order to fetch information about 
available plugins. Since Java (on Windows) can't remove or load jar-files 
while the server is running, ore-get will create a shell-script based on
your server OS that can be executed by your server's watchdog, after the 
server terminated. (This script will only be created if the server did *not* 
crash).

#### On Windows
The script is called `oreget_postserver.bat`. If your watchdog is a batch
script you can add it like this:
```bat
rem start server
java -jar sponge-current-version.jar
rem execute oreget post script
if EXIST oreget_postserver.bat oreget_postserver.bat
```

#### On Linux/Unix
The script is called `oreget_postserver.sh`. If your watchdog is a 
bash-script you can add it like this:
```bash
# start server
java -jar sponge-current-version.jar
# execute oreget post script
test -e oreget_postserver.sh && /bin/bash oreget_postserver.sh
```

![gif](oreget.gif)

# Commands
Prefix is always `/ore-get`, `/oreget` or `/ore`, so a full command looks 
something like this: `/ore-get search huskycrates`   
The base-command requires the permission `oreget.command.base`

* `search QUERY` - **Permission** `oreget.command.search`    
search for QUERY in the Ore repository, like you would on the Website
* `show PLUGINID` - **Permission** `oreget.command.show`    
show detailed information about this plugin
* `install --only-upgrade PLUGINID...` - **Permission** `oreget.command.install`    
install or upgrade one or more plugins by id. if you specify the flag 
`--only-upgrade` no new plugins will be installed
* `upgrade` - **Permission** `oreget.command.upgrade`    
install new versions for all plugins and update dependencies
* `full-upgrade` - **Permission** `oreget.command.fullupgrade`    
like `upgrade`, but removed dependencies that are no longer needed.
* `remove PLUGINID...` - **Permission** `oreget.command.remove`    
mark or unmark the plugin(s) for removal
* `autoremove` - **Permission** `oreget.command.autoremove`    
scan for dependency plugins that are no longer required and mark them for 
removal
* `mark PLUGINID` - **Permission** `oreget.command.mark`    
mark this plugin as dependency, this plugin can now be auto-removed
* `unmark PLUGINID` - **Permission** `oreget.command.mark`
mark this plugin as manually installed, auto-remove won't touch this plugin
* `confirm` - **Permission** `oreget.command.confirm`    
confirm plugin installation/upgrade
* `reject`/`deny`/`cancel` - **Permission** `oreget.command.confirm`    
cancel the plugin installation/upgrade

# Future plans

- [ ] Command fix-deps to automatically fetch missing dependencies for unloaded plugins
- [ ] Command hold, to prevent updates to a specific plugins
- [ ] Command forbid-version, to prever update to a specific version
- [ ] Keep up-to-date with Ore API v2

# External Connections
The connection to Ore is obviously required.  
No other connections, that's all.

### Need Help?
#### [Join my Discord](https://discord.gg/E592Gdu)