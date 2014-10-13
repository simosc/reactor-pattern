var http = require('http');

http.createServer(function(req, res) {
	setTimeout(function() {
		res.writeHead(200);
		res.write("Hejsan");
		res.end();
	}, 3000);
}).listen(9234);