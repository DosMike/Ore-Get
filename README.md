# Ore-Get
A apt-get inspired Plugin management tool.

### How it works
This plugin connects to the Ore API in order to fetch information about 
available plugins. Since Java (on Windows) can't remove or load jar-files 
while the server is running, ore-get will create a shell-script based on
your server OS that can be executed by your server's watchdog, after the 
server terminated. (This script will only be created if the server did *not* 
crash).

You can run this as plugin like any other, or as standalone application.
To run it as standalone, put it into your minecraft server root, mods or 
plugin folder and call it through `java -jar oreget.jar`.

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

# License Stuff

**Fancy terminal colors on Windows in standalone mode**   
_JANSI_ is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0) and available at [https://github.com/fusesource/jansi](https://github.com/fusesource/jansi)   
Copyright &copy; 2009 fusesource

**Processing JSON responses from the Ore API**   
_Gson_ is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0) and available at [https://github.com/google/gson](https://github.com/google/gson)   
Copyright &copy; 2008 Google Inc.

**This Project itself**   
_Ore-Get_ is licensed under the MIT License.
Copyright &copy; 2020 DosMike

### Apache 2.0 License Note  
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

### MIT License
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

### Need Help?
#### [Join my Discord](https://discord.gg/E592Gdu)