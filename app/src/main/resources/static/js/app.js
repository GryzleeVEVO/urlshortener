/*
$(document).ready(
    function() {
        $("#shortener").submit(
            function (event) {
                event.preventDefault();


                $.ajax({
                    type: "POST",
                    url: "/api/link",
                    data: $(this).serialize(),

                    success: function (msg, status, request) {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
                            + "</a></div>");
                    },
                    error: function () {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
    });
*/




$(document).ready(function () {
    $("#shortener").submit(function (event) {
        event.preventDefault();

        // Prove whether QR option is checked
        var generateQrCode = $("#qr").prop("checked");
        console.log(generateQrCode);

        // Serialize Data and put in manually QR Option
        var formData = $(this).serializeArray();
        formData.push({ name: "qr", value: generateQrCode });

        $.ajax({
            type: "POST",
            url: "/api/link",
            data: formData,
            success: function (response, status, request) {
                $("#result").html("<div class='alert alert-success lead'><a target='_blank' href='"
                    + request.getResponseHeader('Location')
                    + "'>Link: "
                    + request.getResponseHeader('Location')
                    + "</a></div>");

                // Extracting hash from URL
                var extractedHash = extractHashFromUrl(response.url);
                console.log(generateQrCode)
                console.log(extractedHash);

                /*// Only calls getQrCode if checkbox is checked
                if (generateQrCode) {
                    console.log("Checkbox is checked"+generateQrCode)
                    getQrCode(extractedHash);
                }*/
                getQrCode(extractedHash, generateQrCode);
            },
            error: function () {
                $("#result").html("<div class='alert alert-danger lead'>ERROR</div>");
            }
        });
    });

    // function to perform GET Request for given [id]. Path: /id/qr
    function getQrCode(id, generateQrCode) {
        if (!generateQrCode) {
            console.log("Checkbox is not checked");
            return;
        }
        console.log("failed weil macht weiter :(")
        $.ajax({
            type: "GET",
            url: "/" + id + "/qr",
            success: function () {
                console.log("AUFGERUFEN!!!")
                //$("#result").append("<div class='alert alert-success lead'>QR Code:<br/><img src='data:image/png" + qrCodeBytes + "' alt='QR Code'/></div>");
                const qrCodeLink = $("<div class='alert alert-success lead'>QR Code Link: <a href='/" + id + "/qr' target='_blank'>/" + id + "/qr</a></div>");
                $("#result").append(qrCodeLink);
            },
            error: function () {
                $("#result").append("<div class='alert alert-danger lead'>Failed to get QR Code</div>");
            }
        });
    }

    //function to extract the hash from a given url
    function extractHashFromUrl(url) {
        // extracts only the Hash from the URL
        var lengthToKeep = 8;
        return url.slice(-lengthToKeep);
    }
});