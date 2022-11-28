var config = {};

function buildPage() {
	for (const wij of config.widgets) {
		var frame = $("#widgetTemplates #t_frame").clone();
		$("#content").append(frame);
		frame.css("display", "inline-block");
		frame.find(".title").text(wij.name);
	}
}

$(document).ready(function() {
	//
	// get config from server
	//
	var configname = getUrlParameter("config");
	if (!configname)
		configname = "default";
	$.ajax({url:"config?name=" + configname}).done(function(res) {
		config = $.parseJSON(res);
		buildPage();
	});
});

