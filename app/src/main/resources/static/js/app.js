$(document).ready(
    function () {
        $("#shortener").submit(
            function (event) {
                event.preventDefault();

                // Prove whether QR option is checked
                var qrCodeRequested = $("#qr").prop("checked");
                //console.log(qrCodeRequested);

                // Serialize Data and put in manually QR Option
                var formData = $(this).serializeArray();
                formData.push({ name: "qr", value: qrCodeRequested });

                $.ajax({
                    type: "POST",
                    url: "/api/link",
                    data: formData,
                    success: function (msg, status, request) {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
                            + "</a></div>");

                        // Extraer el valor hash del final de la URL
                        var extractedHash = extractHashFromUrl(msg.url);
                        //console.log(qrCodeRequested)
                        //console.log(extractedHash);
                        getQrCode(extractedHash, qrCodeRequested);

                        // Comprobar si la casilla de verificación está seleccionada antes de que se muestre el código QR
                        /*
                        if (qrCodeRequested) {
                            getQrCode(extractedHash);
                        }*/
                    },
                    error: function () {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });

        // Manejar el envío del formulario CSV
        $("#csvForm").submit(function (event) {
            event.preventDefault();
            var formData = new FormData($(this)[0]);

            $.ajax({
                type: "POST",
                url: "/api/bulk",
                data: formData,
                contentType: false,
                processData: false,
                success: function (data, request) { //, request
                    // La variable "data" contiene la respuesta del servidor.
                    // Manejar la respuesta del servidor con el contenido del CSV procesado
                    var resultHtml = "<div class='alert alert-success lead'>CSV procesado:<br>";
                    resultHtml += "<a href='data:text/csv;charset=utf-8," + encodeURIComponent(data) + "' download='ShortUrlCollection.csv'>Download CSV</a>";
                    resultHtml += "</div>";

                    $("#result").html(resultHtml);
                },
                error: function () {
                    $("#result").html("<div class='alert alert-danger lead'>Error al procesar el CSV</div>");
                }
            });
        });

        // function to perform GET Request for given [id]. Path: /id/qr
        function getQrCode(id, generateQrCode) {
            if (!generateQrCode) {
                //console.log("Checkbox is not checked");
                return;
            }

            $.ajax({
                type: "GET",
                url: "/" + id + "/qr",
                success: function () {
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
            // partir la URL por '/'
            var parts = url.split('/');

            // Tomar el último fragmento resultante
            var lastPart = parts[parts.length - 1];

            return lastPart;
        }
    });