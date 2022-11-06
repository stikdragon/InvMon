
const chart_CHARTNAME = new Chart(document.getElementById("CHARTNAME").getContext("2d"), {
	type: 'line',
	data: {
		labels: dTIMESTAMP,
		datasets: [
			DATASETS
		]
	},
	options: {
		bezierCurve: false,
		responsive: false,
		lineTension: 0,
		// Can't just just `stacked: true` like the docs say
		scales: {
			yAxes: [{
				stacked: true,
			}]
		},
		animation: {
			duration: 750,
		},
	}
});