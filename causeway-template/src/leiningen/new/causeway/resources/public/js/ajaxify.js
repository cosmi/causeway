$(document).ready( function() {

    $("body").on("click", ".ajaxify, .ajaxify-get, .ajaxify-put, .ajaxify-delete", function(e) {
        var $this = $(this);
        var url = $this.attr("href") || $this.data("url");
        var onSuccess = $this.hasClass("ajaxify-ignore")?"ignore":$this.hasClass("ajaxify-show")?"show":"reload";
        var method = 
            $this.hasClass("ajaxify-get")?"get":
            $this.hasClass("ajaxify-put")?"put":
            $this.hasClass("ajaxify-delete")?"delete":"post";
        var confirmQuestion = $this.data("confirm");

        if(!confirmQuestion || confirm(confirmQuestion)) {
            $.ajax(url, {method:method, 
                         success: function(data) {
                             switch(onSuccess) {
                             case "ignore" : return;
                             case "show" : document.body.innerHtml(data); return;
                             case "reload" : window.location.reload();
                             }
                         },
                         error: function(xhr, status, msg) {
                             alert(msg);
                         }});
        }

        e.preventDefault();
    })

});
