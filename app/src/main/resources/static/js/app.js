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

        // Prüfen, ob die Checkbox ausgewählt ist
        var generateQrCode = $("#qr").prop("checked");
        console.log(generateQrCode);

        // Daten für die Serialisierung vorbereiten
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

                // Extrahieren Sie den Hash-Wert vom Ende der URL
                var extractedHash = extractHashFromUrl(response.url);

                console.log(extractedHash);

                // Prüfen, ob die Checkbox ausgewählt ist, bevor der QR-Code angezeigt wird
                if (generateQrCode) {
                    getQrCode(extractedHash);
                }
            },
            error: function () {
                $("#result").html("<div class='alert alert-danger lead'>ERROR</div>");
            }
        });
    });

    // Funktion zum Abrufen des QR-Codes
    function getQrCode(id) {
        $.ajax({
            type: "GET",
            url: "/" + id + "/qr",
            success: function (qrCodeBytes) {
                $("#result").append("<div class='alert alert-success lead'>QR Code:<br/><img src='data:image/png;base64," + qrCodeBytes + "' alt='QR Code'/></div>");
            },
            error: function () {
                $("#result").append("<div class='alert alert-danger lead'>Failed to get QR Code</div>");
            }
        });
    }

    function extractHashFromUrl(url) {
        // Hier die Anzahl der Zeichen, die Sie behalten möchten (in Ihrem Fall 8)
        var lengthToKeep = 8;
        return url.slice(-lengthToKeep);
    }
});