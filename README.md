# Web_Sever_JAVA


Server Features:

• GET —---------------------------------  Get a document from the server.

• HEAD —-------------------------------   Get the headers for a document from the server.

• Logging —----------------------------   Text document that contains a record of all activity

• Multi-Threading —--------------------   Handle more than one thread.

• POST —------------------------------    Send data to the server for processing.

• OPTIONS —--------------------------     Determine what methods can operate on a server.

• A server administration interface (UI)

• Authentication/authorization



Files:

All the files are located under 389F folder:

• JHTTP.java —-------------------------- Handle Logging, Multi-Threading.

• RequestProcessor.java----------------- Handle GET, HEAD, POST, OPTIONS request.

• form.html —--------------------------- Test out GET/POST/HEAD request.

• index.html —--------------------------- Test out administration UI & Authentication.

• syr_ecs.svg & syr_bg.jpg ---------------Images for administration UI.




You might want to download CURL as the command line tool to test out some of the server

features. Below is a link showing how to download CURL to your device.

For Windows User: https://reqbin.com/req/c-g95rmxs0/curl-for-windows

For Mac User: https://help.ubidots.com/en/articles/2165289-learn-how-to-install-run-curl-on windows-macosx-linux



How to Compile and Run the server (for MacOS)

Downloaded 389F folder to your device.

Open terminal, find where you stored the 389F folder, cd 389F to open it.

Once you are inside the folder, insert javac JHTTP.java to compile the files.

Next, insert java JHTTP ~/Desktop/389F 777 to run the server.

~/Desktop/389F should be changed corresponding to where you stored 389F.

777 is the port number which could be changed.




Test out Logging

Once the server starts, you should receive the following logging message directly from the server side.


Dec 08, 2022 2:02:51 PM JHTTP start

INFO: Accepting connections on port 777

Dec 08, 2022 2:02:51 PM JHTTP start

INFO: Document Root: /Users/yjs/Desktop/389F




Test out GET / POST request

Method 1:

enter http://localhost:777/ to your browser, 777 should change to the port number
you are using. Now you should receive the GET request on the server side.

There are two accounts that you can try for the Authentication/authorization.

Other accounts will not be accepted by the server.

ID: admin Password: 000000

ID: user01 Password: 111111

Once you click the Log In button, you will receive the POST request on the server.

Admin UI will show up when you log in as Admin. Otherwise, you will see the regular user UI.

Method 2:

Enter http://localhost:777/form.html will lead you to another website.

Enter your rating of the day on the website, then click Submit.

It will return you the POST message and more details about the request.


Test out Multi-Threading

Try open http://localhost:777/ at different web browsers. You will find out the server

can handle more than one user at a time.



Test out HEAD/GET/POST/OPTIONS using CURL

Run those command lines on Terminal or similar applications.

Make sure you do this in a new window (not the same window as your server)

Make sure you download CURL & Change 777 to your port number.

After you run the command lines, you will receive the corresponding response from server and
log messages on the server.



HEAD Request:

1 curl -I http://localhost:777

1 curl -I http://localhost:777/form.html

GET Request

1 curl http://localhost:777

POST Request

1 curl -X POST http://localhost:777

OPTIONS Request

1 curl -i -X OPTIONS http://localhost:777















