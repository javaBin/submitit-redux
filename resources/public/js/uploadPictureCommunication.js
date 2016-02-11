$(document).ready(function() {
    var dummyKey = $("#dummyKey").val();
    var speakerKey = $("#speakerKey").val();
    parent.pictureChanged(dummyKey,speakerKey);
    console.log("sdgtsdg");

    $("#uploadBtn").click(function(event) {
        var sizeinbytes = document.getElementById('filehandler').files[0].size;
        if (sizeinbytes > 500000) {
            $("#messagePara").text("File to large (max 500k)");
            event.preventDefault();
        }
    });
});
