<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%--
The taglib directive below imports the JSTL library. If you uncomment it,
you must also add the JSTL library to the project. The Add Library... action
on Libraries node in Projects view can be used to add the JSTL 1.1 library.
--%>
<%--
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 
--%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Stock Quote Client Sample</title>
    </head>
    <body>

    <h1>Stock Quote Client Sample</h1>
    
    <form name="GetQuote" action="GetQuote" method="GET">
        Stock Symbol: <input type="text" name="symbol" value="SUNW" size="12" />
        <p><input type="submit" value="GetQuote" name="quote" />
    </form>
    <p><hr>
    <form name="AMConsole" action="/amserver/console" method="GET">
        Click <a href="/amserver/console">here</a> to view Access Manager Console
        <p><input type="submit" value="AMConsole"/>
    </form>
    
    </body>
</html>
