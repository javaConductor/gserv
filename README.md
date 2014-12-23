<div  ng-controller="GServGeneralDocsCtrl">
        <h2 class="docs-sub-header">What is gServ?</h2>
        <span class="docs-content">
            gServ is a tool for creating and deploying REST based services using Groovy without the
            hassle of a container (JBoss, Tomcat, etc.).  Using gServ, you can easily define
            REST resources as Groovy scripts or embed  gServ in your application. gServ is
            perfect for creating lightweight micro services.
        </span>

            <br/><h3>Features:</h3>
            <ul class="docs-content">
                <li>Container Free
                <li>Serve static files
                <li>Serve Groovy script as REST resources
                <li>Plugin API
                <li>Embeddable
                <li>Standalone Mode
                <li>CORS support
                <li>Compression support
                <li>ETag support
                <li>Basic Authentication
                <li> HTTPS
            </ul>
                <h3>Requirements:</h3>
                <ul><li>Java JDK 1.6+</ul>

    <div class="docs-content">
        <h3>Basic Concepts</h3>
        <table width="90%" class="docs-content">
            <tr><th>Term</th>             <th>Meaning</th></tr>
            <tr><td>Resources</td>      <td>Resources define actions - one for each HTTP method. </td></tr>
            <tr><td>Server Config</td><td>The config encapsulates any resources, end-points, filters, and plugins. </td></tr>
            <tr><td>Server Instance</td><td>This is the actual server that will listen to the specified port and handle
                requests based on its configuration.</td></tr>
        </table>
    </div>

</div>
<span class="docs-sub-header">gServ can be used in two ways</span><br/>
[Framework](https://github.com/javaConductor/gserv/wiki/gServ-Framework)<br/>
[Standalone](https://github.com/javaConductor/gserv/wiki/gServ-Standalone)
#Simple Examples

<table>
<tr><th colspan='2'>
Creating REST Resources
</th></tr>
<tr><td width='60%'>
<pre>

/// create a GServ instance
def gserv = new GServ()

/// Create a Books REST resource
def bkResource = gserv.resource("/books") {
    // URI:  /books/faq
    get “/faq”, file(“BooksFaq.html”)
    
    // URI: /books/xyz
    get “:id”, { id ->
        def book = bookService.get( id )
        writeJson book
    }
    
    // responds  to /books/all
    get “/all”, {  ->
        def books = bookService.allBooks ()
        header “content-type”, “application/json”
        writeJSON books
    }
}

</pre>
</td>
<td width='40%'>
The root path is passed to the GServ.resource() method along with a closure defining the actions for the resource.
</td>
</tr>
<tr>
<th colspan='2'>
Creating a Server Instance
</th>
</tr>
<tr>
<td>
<pre>

gserv.http {
    // setup a directory for static files
    static_root  '/public/webapp'

    //static FAQ page located at '/public/webapp/App.faq.html'
    get '/faq', file("App.faq.html")
    
}.start(8080);


</pre>
</td>
<td>
The http() method creates a GServInstance that can later listen on a port and handle HTTP requests. This server instance
defines static roots usually used for templates for single-page apps and a single FAQ page.
Then, after the server instance is returned from the http() method, we can immediately call start(8080) on it.
</td>
</tr>
<tr><th colspan='2'>
Adding Resources to a Server Instance
</th>
</tr><tr>
<td>
<pre>

def bkResource = gserv.resource("/books") { ... }
def userResource = gserv.resource("/users") { ... }

gserv.http {
    // setup a directory for static files
    static_root "/public/webapp" 

    // static FAQ page located at '/public/webapp/App.faq.html'
    get '/faq', file('App.faq.html')

    /// add Book and User REST resources to our GServ instance
    resource bkResource
    resource userResource
 }.start(8080);

</pre>
</td>
<td>
A server instance can be created by simply adding resources.  Here we add our 2 resources: bkResources and
userResources.  Now, all URIs related to both resources are available once the instance is started. This instance also
defines a static_root which tells gserv where to find static files such as the FAQ page which should be at /public/webapp/App.faq.html.
</td>
</tr>
</table>
