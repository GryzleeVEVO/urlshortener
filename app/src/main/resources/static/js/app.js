$(document).ready(
    function () {
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
                success: function (data) {
                    // La variable "data" contiene la respuesta del servidor.
                    // Manejar la respuesta del servidor con el contenido del CSV procesado
                    var resultHtml = "<div class='alert alert-success lead'>CSV procesado:<br>";
                    resultHtml += "<a href='data:text/csv;charset=utf-8," + encodeURIComponent(data) + "' download='output.csv'>Download CSV</a>";
                    resultHtml += "</div>";

                    $("#result").html(resultHtml);
                },
                error: function () {
                    $("#result").html("<div class='alert alert-danger lead'>Error al procesar el CSV</div>");
                }
            });
        });
    });