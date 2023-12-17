$(document).ready(
    function () {
        $("#shortener").submit(
            function (event) {
                event.preventDefault();

                // Comprobar si la casilla de verificacion esta seleccionada
                var generateQrCode = $("#qr").prop("checked");
                console.log(generateQrCode);

                // Preparar los datos para la serializacion.
                var formData = $(this).serializeArray();
                formData.push({ name: "qr", value: generateQrCode });

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
                        var extractedHash = extractHashFromUrl(response.url);

                        console.log(extractedHash);

                        // Comprobar si la casilla de verificación está seleccionada antes de que se muestre el código QR
                        if (generateQrCode) {
                            getQrCode(extractedHash);
                        }
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

        // Función de recuperación de código QR
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
            // Número de caracteres a guardar, 8
            var lengthToKeep = 8;
            return url.slice(-lengthToKeep);
        }
    });