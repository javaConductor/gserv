#    g-serv

##Framework for building single-page aps and REST based services.

####Uses 1.6 HTTP Server from Java (com.sun.net.httpserver.HttpServer), therefore, Java 6+ is required.


###Getting Started
You will need to install Groovy if it is not already installed.


###Creating REST Resources:

```

/// create a GServ instance
def gserv = new GServ()

/// Create a Books REST resource
/* the root path is passed to the GServ.resource() method
along with a closure defining the endpoints or the resource
*/
def bkResource = gserv.resource("/books") {
    ////
    // responds  to /books/faq
    get(“/faq”, file(“BooksFaq.html”))
    // responds  to /books/xyz
    get(“:id)”, { id ->
        def book = bookService.get( id )
        writeJson(book)
    }
    // responds  to /books/all
    get(“/all”, {  ->
        def books = bookService.allBooks ()
        header(“content-type”, “application/json”)
        writeJSON(books)
    })
}

// The http() method creates a GServInstance that can later listen on a port and handle REST requests

gserv.http {
    // setup a directory for static files
    static_root ("webapp")

    /// add Book REST resources to our GServ instance
    resource(bkResource)
}.start(8080);
```

## Services 

##Creating Services

Creating services in g-serv is extremely simple.

The following code creates an HTTP server and starts it listening to port 8080:

```
import com.soulsys.gserv.GServ
class WelcomeService {

    public static void main(String[] args) {
        new WelcomeService();
    }

    def WelcomeService() {
    def gserv = new GServ()

        gserv.http {
            get("/welcome", {->
                write("Welcome to g-serv, The embeddable REST server for Groovy!")
            });

        }.start(8080)
    }
}
```
##Usage

Copy this code to WelcomeService.groovy, compile and run it. Then navigate your browser
to [http://localhost:8080/welcome]. You should see the welcome message above in your browser.

You may add a single resource / method handler to the service but its usually more useful to group the operations in a resource together.  So, a more realistic example would be:

```
def userService
def bookService

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

gserv.http {
    static_root (‘webapp’)
    resource( bookResource )
    resource( userResource )
}.start(8080);

```

Services are composed of resources, staticRoots and filters. 
The service definition closure hs access to the following functions:
```
static_root()
resource()
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

