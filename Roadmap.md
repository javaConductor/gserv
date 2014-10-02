#1) Expand the route defs to include wildcards: /thing/other([^\d+])/

#2) Create the command line interface

    -- Just serve the static 
    gserv [Static Path] [Port]  
    
    OR
    
    -- Pass the static path to an App using gServ
    myApp [Static Path] [port]
    
    OR
    
    -- Pass the static path and load the resources from the [ClassPath] 
    gserv [Static Path]  [ClassPath?] [port]
    
    
##2a) gserv [Static Path] [Port]
    Just needs to do:
        staticRoots(StaticPath)
    in the Service config.
    Then start on the specified port.
    
    Solution: 
        - create the HTTP(s)ServerInstance.
        - add staticPath to static-roots of config
        - start the server with the cfg on specified port    

    How: - - - - - - - - - - - ????

    Create an object called GServRunner which has:
    - createConfig( staticRoot, port) : GServConfig
    - start(staticRoot, port, defaultPage) : gServInstnace

    Handling the default page!!!

    Version 1:
    Send back a 302 redir to the defaultPage
    This is done by the InitFilter.

    Version 2:
    Add the [get('\', file(defaultPage) )] to the server config
    This must be done in the GServRunner as the config is created

    
##2b) myApp [Static Path] [Port]
    Create an app using gServ.  From the command-line, set the staticRoot(s),
    Start at specified port.

##2c) gserv [Static Path] [Port] [ClassPath?]
    Totally generic. Can be used with any Jar that was built as Resource jar.
    Start at specified port.
    How is a Resource Jar created ?? ?? ??
     

---------------------------------------------------------------------------------------------------------------------
#LATER:
##Implement -> GServInstance.on('event', function(){}) // func / closure
    This should fire life-cycle events

