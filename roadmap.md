#gServ Framework Roadmap

##0.9.7 - 3/2/2015

#gServ App

##0.9.7

#gServTest

##0.9.7


#gRestTest

Usage:

def inbound = server('inboundTraffic') { 
    startCommand: "gradlew run",
    stopCommand: "kill -9 $pid"
}
def t1 = testSuite {
    request: [
        method:'PUT',
        url: 'http://localhost:8090/Traffic/healthcheck',
        data: "Hello World".bytes,
        headers : [
            Content-Type : ['application/xml']
        ]
    ],
    test: { response -> 
        assert response.statusCode == 200
          
    }
}

withService(inbound).test( [ t1,t2] )
