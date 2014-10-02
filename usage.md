#    g-serv

##Embeddable asynchronous HTTP server for Groovy asynchronous.

###Usage:

#### Creating Resources
Here we create 2 resources /books and /users.
```
@Inject
def bookService
@Inject
def userService

/// create a GServ instance
def gserv = new GServ()
def userResource = gserv.resource("/users"){
    get {id ->
     def u=service.getUser(id)
     template("user.html", u)
     }
    put { istream ->

        def user = to.json(istream) as User
        service.updateUser( user )
        template("user.html", user)
    }

    post { istream ->
        def u = to.json(istream) as User
        service.createUser(u)
        template("user.html", u)
    }

    delete(/:id) { id ->
        def bk = service.userBook(id)
        writeJson{[ok:true])
    }
}

def bookResource = gserv.resource("/books"){

    get("/:id" ){ id ->
     def bk=service.getBook(id)
     template("book.html", bk)
     }
    put { istream ->
        def bk = to.json(istream) as Book
        service.updateBook(bk)
        template("book.html", bk)
    }
    post { istream ->
        def bk = to.json(istream) as Book
        service.createBook(bk)
        template("book.html", bk)
    }
    delete(/:id) { id ->
        def bk = service.removeBook(id)
        writeJson{[ok:true])
    }
}
```
#### Creating Server Instance
```gserv.http {
    /// Setup a directory accessible from the fileSystem for static files
    static_root ("webapp")
    /// add Book and User REST resources to our GServ instance
    resource (userResource)
    resource(bookResource)
}.start(8080); // start listening for requests on port 8080
```
Here the http() method creates a GServInstance that can later listen on a port and handle REST requests

#### Server Config Built-in Functions
```
static_root(directoryInFileSystem) - defines a directory to resolve requests for static files.

resource( GServeResource resource ) - defines a REST resource for a URI prefix

/// can also include "stray" routes
get()
put()
post()
delete()
filter()
cors()

```
also:
```
file()
```

##Defining Resources

Here we use GServ.resource to define a REST resource.   The resource definition closure has methods for each of the HTTP commands GET, PUT, POST, DELETE. Methods include:
```
write( ... )
forward()
call()
template()
to.[json|xml](istream)
```


    [delete|post|put|get]( URI ){ [istream,] routeVar1, ... routeVarN ->
        
         ... resource code here ...
    
    }


##Defining Filters
Server Config  may include a Filter definitions.
The builtin function "filter( )" is used to define a filter.
As expected, filters get called before the Route code is called.
The filter may choose to: call a different Route, return a response or error, pre/post process the output of the Route processing.
A filter may be passive and record activity rather than effect the processing.


##Config Statements  by Config Type
### Server Config
#### get
syntax:
```
get ("path/:var1/:var2") { var1, var2 ->
//// handle request
}
```
#### put
syntax:
```
put ("path/:var1/:var2") { inputStream, var1, var2 ->
//// handle request
}
```

#### post

syntax:
```
post ("path/:var1/:var2") { inputStream, var1, var2 ->
//// handle request
}
```
#### delete
syntax:
```
delete ("path/:var1/:var2") { var1, var2 ->
//// handle request
}
```

