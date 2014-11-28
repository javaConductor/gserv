# Domain Definitions (Ubiquitous Language)

### Path
The URL path with or without embedded variables.

### Route
A path, HTTP method and the associated HTTPMethodHandler.
Routes are defined by the: get(), put(), post(), and delete() functions available in any serverConfig or resource definition.

### Server Resource
A collection of Resource Actions for a single URL prefix.
Server Resources are created by calling resource() on a GServ instance and passing the Path and a Resource Closure as arguments.

###  ServerConfig
Defines filters, resources, actions, plugins, and static roots for a ServerInstance.


###  Server Instance
This is an instance of GServ initialized with a ServerConfig object.
Once created, a server instance can be started on a specified port.

###  HTTPMethodHandler
This closure handles the requests for the corresponding resource

### Resource Config
A closure defining a Server Resource.  It consists of a root path and one or more Routes derived from that path.
Resource config closures have builtin functions for defining actions.

### FilterProxy

### Matcher
Responsible for matching request URL with Routes
NOTE: Query params are not being matched against

### Plugin
Plugins are used to extend the functionality of GServ.
A plugin may add:
 - functions and values to the ServerConfig.
 - filters to modify the behavior of default request processing.
 - staticRoots to add its own views or static data

Packaged as JAR.
An Object (usually implements IPlugin) containing a function for altering the closure delegate's methods
and properties, add filters, add staticRoots,
