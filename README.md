Webpage: juneau.cs.colostate.edu:45300

This project contains two seperate spark programs and a Go driven web server.

- mapBuilder/src/main/java/mapBuilder.java:
  mapBuilder houses the java based spark program. This program opens a socket on
  one of the worker nodes and then ingests pairs of latitude, longitude data.
  returns a subset of the data that is turn into a heatmap on the clients browser.

- mapBuider/pom.xml
  builds java mapBuilder

- emailLDA/src/main/scala/Analysis.scala
  Analysis houses the scala based spark program. This program opens a socket on
  one of the worker nodes and then ingest strings of subject lines from email data.
  Returns 10 lines of categorized email subject lines.

- emailLDA/build.sbt
  builds scala emailLDA

- term-project-server.go
  Go webserver that servers webpage on juneau.cs.colostate.edu:45300.
  Webserver takes javascript post requests with data type and then uses
  shotgun approach to connect and send data to one of the worker nodes
  in the cluster. Then server reads data back from spark program and 
  forwards the results back to the client.

- index.html
  Webpage for interacting with the server.
  
