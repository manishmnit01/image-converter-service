<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page session="false" %>
<html>
<head>
<title>Upload File Request Page</title>
</head>
<body>
    <h3>Upload image to convert it to JPEG</h3>
    Select a file to upload: <br />
	<form method="POST" action="uploadFile" enctype="multipart/form-data">
		<input type="file" name="file" size = "50">
		<br /> <br />
		<input type="submit" value="Upload">
	</form>
</body>
</html>