var layout = {};
var frames = {};


function updateChart(id, enableGlass) {
	var cont = $("#" + id + " .content");
	var hdr = $("#" + id + " .hdr");
	var gls = $("#" + id + " .glass");
	var opts = { name: id, w: cont.width(), h: cont.height() };
	$("#" + id + " .error").hide();
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
	}).fail(function(xhr, status, msg) {
		$("#" + id + " .error .message").text(msg);
		$("#" + id + " .error").show();
	}).always(function() {
		if (enableGlass) {
			gls.css("background-color", "rgba(0, 0, 0, 0%)");
			gls.hide();
		}
	});
}

function buildTimeSel(id) {
	var cont = $("#" + id + " .content");
	cont = cont.append("<div class=\"controls\"></div>").children(".controls");

	let ranges = [
		[5, "5 Min"],
		[30, "30 Min"],
		[60, "1 Hour"],
		[120, "2 Hour"],
		[360, "6 Hour"],
		[720, "12 Hour"],
		[1440, "24 Hour"],
		[2880, "2 Day"],
		[7200, "5 Day"],
		[43200, "30 Day"],
	];

	for (const rng of ranges) {
		cont.append("<a href=\"#\" data-len=\"" + rng[0] + "\">" + rng[1] + "</a>");
		cont.append("<div class=\"divider\"></div>");
	}
	cont.children("a").addClass("unsel").click(function(ev) { setTimeRange($(ev.target).data("len")); });
}

function setTimeRange(range) {
	$.ajax({ url: "setParams?" + JSON.stringify({ off: 0, dur: range }) }).done(
		function(ok) {
			refreshAll(true);
		}
	);
	$(".controls a").each(function(idx) {
		const x = $(this);
		x.toggleClass("sel", x.data("len") == range);
		x.toggleClass("unsel", x.data("len") != range);
	});
}

function refreshAll(glass) {
	if (layout.widgets) {
		for (const wij of layout.widgets) {
			if (frames[wij.id].update) {
				frames[wij.id].update(glass);
			}
		}
	}
}

function buildPage() {
	for (const wij of layout.widgets) {
		let frame;
		switch (wij.type) {
			case "infobit":
				frame = $("<div />");
				frame.build = function() { frame.append(layout["infoBit"]); }
				break;
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

$(window).on("load", function() {
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

