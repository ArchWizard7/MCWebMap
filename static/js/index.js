$(function() {
    const environmentInput = $("#environment-input");
    const latInput = $("#lat-input");
    const lngInput = $("#lng-input");
    const zoomInput = $("#zoom-input");
    const sendButton = $("#send-button");

    let environment = environmentInput.val();

    console.log(`ENVIRONMENT = ${environment}`);

    sendButton.on("click", function() {
        environment = (environmentInput.val().length > 0) ? environmentInput.val() : "overworld";
        const latitude = (latInput.val().length > 0) ? latInput.val() : 0.0;
        const longitude = (lngInput.val().length > 0) ? lngInput.val() : 0.0;
        const zoom = (zoomInput.val().length > 0) ? zoomInput.val() : 18;

        console.log(`(\u001b[31m${latitude}\u001b[0m, \u001b[32m${longitude}\u001b[0m, \u001b[33m${zoom}\u001b[0m)`);

        map.setView([latitude, longitude], zoom);
    });

    // Leaflet //

    const map = L.map("map", {
        center: [0.0, 0.0],
        zoom: 18,
        minZoom: 14,
        maxZoom: 18,
        attribution: "© ArchWizard7"
    });

    map.on("click", function (e) {
        const lat = e.latlng.lat;
        const lng = e.latlng.lng;
        latInput.val(lat);
        lngInput.val(lng);
    });

    L.tileLayer(`/static/tiles/${environment}_{z}_{x}_{y}.png`, ).addTo(map);

    addWaypoints(map, environment); // ウェイポイントを地図上に追加
});

function addWaypoints(map, environment) {
    let CustomIcon = L.Icon.extend({
        options: {
            iconSize:     [32, 32],
            iconAnchor:   [16, 16],
            popupAnchor:  [0, 0]
        }
    });

    $.ajax({
        url: `/get-waypoints?environment=${environment}`,
        type: "GET"
    }).done(function(data) {
        const json = JSON.parse(JSON.stringify(data));

        for (let i = 0; i < json.length; i++) {
            const obj = json[i];
            const name = obj["name"];
            const registered = obj["registered"];
            const date = obj["date"];
            const x = obj["x"];
            const y = obj["y"];
            const z = obj["z"];
            const icon = obj["icon"];

            const offset = 0.0006866455077859351;
            const ratio = 93252.2914041;

            const iconObj = new CustomIcon({
                iconUrl: `/static/icon/${icon}.png`
            });

            const mapMarker = L.marker([(z / (ratio * -1)) - offset, (x / ratio) + offset], {
                icon: iconObj
            }).addTo(map);
            const comment = `
                <h3>${name}</h3>
                <ul>
                    <li>登録者：
                        <img src="https://mc-heads.net/avatar/${registered}/16" alt="Avatar">
                    </li>
                    <li>登録日時：${date}</li>
                    <li>座標：<b>(${x}, ${y}, ${z})</b></li>
                    <li>Leaflet座標：<b>(${(z / -97673.5338731)}, ${(x / 89288.5097806)})</b></li>
                </ul>
            `;
            mapMarker.bindPopup(comment);
        }
    }).fail(function (a, b, c) {
        console.log(a);
        console.log(b);
        console.log(c);
    });
}