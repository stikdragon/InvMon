var layout = {};
var frames = {};


function updateChart(id, enableGlass) {
	var cont = $("#" + id + " .content");
	var hdr = $("#" + id + " .hdr");
	var gls = $("#" + id + " .glass");
	var opts = { name: id, w: cont.width(), h: cont.height() };
	if (enableGlass) {
		gls.css("background-color", "rgba(0, 0, 0, 50%)");
		gls.show();
	}
	$.ajax({ url: "getSectData?" + JSON.stringify(opts) }).done(function(res) {
		var obj = JSON.parse(res);
		cont.empty();
		hdr.children(".grp").remove();
		cont.html(obj["contentHtml"]);
		for (const s of obj["titleBits"])
			hdr.append(s);
		if (enableGlass) {
			gls.css("background-color", "rgba(0, 0, 0, 0%)");
			gls.hide();
		}
	});
}

function buildTimeSel(id) {
	var cont = $("#" + id + " .content");
	cont = cont.append("<div class=\"controls\"></div>").children(".controls");
	cont.append("<a href=\"#\" data-len=\"5\">5 Min</a>");
	cont.append("<a href=\"#\" data-len=\"30\">30 Min</a>");
	cont.append("<a href=\"#\" data-len=\"60\">1 Hour</a>");
	cont.append("<a href=\"#\" data-len=\"120\">2 Hour</a>");
	cont.append("<a href=\"#\" data-len=\"720\">12 Hour</a>");
	cont.append("<a href=\"#\" data-len=\"1440\">24 Hour</a>");
	cont.append("<a href=\"#\" data-len=\"7200\">5 Day</a>");
	cont.append("<a href=\"#\" data-len=\"43200\">30 Day</a>");

	cont.children("a").addClass("unsel").click(function(ev) { setTimeRange($(ev.target).data("len")); });
}

function setTimeRange(range) {
	$.ajax({ url: "setParams?" + JSON.stringify({ off: 0, dur: range }) }).done(
		function(ok) {
			refreshAll(true);
		}
	);
}

function refreshAll(glass) {
	for (const wij of layout.widgets) {
		if (frames[wij.id].update) {
			frames[wij.id].update(glass);
		}
	}
	
}

function buildPage() {
	for (const wij of layout.widgets) {
		let frame;
		switch (wij.type) {
			case "timesel":
				frame = $("#widgetTemplates #t_timesel").clone();
				frame.update = null;
				frame.build = function() { buildTimeSel(wij.id); };
				break;
			default:
				frame = $("#widgetTemplates #t_frame").clone();
				frame.find(".title").text(wij.name);
				frame.update = function(glass) { updateChart(wij.id, glass); };
				frame.build = null;
		}
		frame.prop("id", wij.id);
		frame.css("display", "inline-block");
		frame.css({ top: wij.y * layout.gridSize, left: wij.x * layout.gridSize, width: wij.w * layout.gridSize, height: wij.h * layout.gridSize, position: "absolute" });
		$("#content").append(frame);
		if (frame.build)
			frame.build();
		if (frame.update)
			frame.update(true);
		frames[wij.id] = frame;
	}
}

$(document).ready(function() {
	//
	// get config from server
	//
	var configname = getUrlParameter("config");
	if (!configname)
		configname = "default";
	$.ajax({ url: "getConfig?name=" + configname }).done(function(res) {
		layout = $.parseJSON(res);
		buildPage();
	});

	//
	// current url params to session
	//
	var dur = getUrlParameter("dur");
	if (!dur)
		dur = 240;
	setTimeRange(dur);
});

