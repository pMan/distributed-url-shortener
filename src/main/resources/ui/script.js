$(document).ready(function () {

    var shortenResults = $("#results");
    var restResults = $("#rest-results");
	
    $("#submit").on("click", function () {
        if ($("#url").val().trim() == "") {
            alert("Provide a valid URL.");
            $("#url").focus();
            return;
        }
        $.ajax({
            method: "POST",
            data: $("#url").val().trim(),
            url: "shorten",
            success: onUrlShortenResponse
        });
    });

    $("#links button").on("click", function (e) {
        $.ajax({
            method: "GET",
            url: e.target.id,
            success: onRestResponse
        });
    });

	function onRestResponse(data, status) {
        if (status === "success") {
            restResults.text(JSON.stringify(data, null, 2)).show();
        } else {
            alert("Error connecting to the server " + status);
        }
    }
    
    function onUrlShortenResponse(data, status) {
        if (status === "success") {
            var shortUrl = data.url;
            shortenResults.html('<a target="_blank" href="' + shortUrl + '">' + shortUrl + '</a');
            shortenResults.show();
        } else {
            alert("Error connecting to the server " + status);
        }
    }
});
