var APP = (function() {
    var map = (function() {
        var m = L.map('map').setView(GTT.Constants.VIEW_COORDS, 12);
        GTT.BaseLayers.addTo(m);

        $('#map').resize(function() {
            m.setView(m.getBounds(),m.getZoom());
        });

        return m;
    })();

    var requestModel = GTT.createRequestModel();

    var transitShedLayer = (function() {
        var mapLayer = null;
        var opacity = 0.9;

        var update = function() {
            var modes = requestModel.getModesString();
            if(modes != "") {
                var latLng = requestModel.getLatLng();
                var time = requestModel.getTime();
                var direction = requestModel.getDirection();
                var schedule = requestModel.getSchedule();
                var dynamicRendering = requestModel.getDynamicRendering();

                if (mapLayer) {
//                    map.lc.removeLayer(mapLayer);
                    map.removeLayer(mapLayer);
                    mapLayer = null;
                }

		if(dynamicRendering) {
                    var url = GTT.Constants.BASE_URL + "/travelshed/wmsdata";
		    mapLayer = new L.TileLayer.WMS2(url, {
                        getValue : function() { return requestModel.getDuration(); },
                        latitude: latLng.lat,
                        longitude: latLng.lng,
                        time: time,
                        duration: GTT.Constants.MAX_DURATION,
                        modes: modes,
                        schedule: schedule,
                        direction: direction,
                        breaks: GTT.Constants.BREAKS,
                        palette: GTT.Constants.COLORS,
                        attribution: 'Azavea'
		    });
		} else {
                    var url = GTT.Constants.BASE_URL + "/travelshed/wms";
		    mapLayer = new L.TileLayer.WMS(url, {
                        latitude: latLng.lat,
                        longitude: latLng.lng,
                        time: time,
                        duration: requestModel.getDuration(),
                        modes: modes,
                        schedule: schedule,
                        direction: direction,
                        breaks: GTT.Constants.BREAKS,
                        palette: GTT.Constants.COLORS,
                        attribution: 'Azavea'
		    });
                }
		
		mapLayer.setOpacity(opacity);
		mapLayer.addTo(map);
//		map.lc.addOverlay(mapLayer, "Travel Times");
            }
        };

        requestModel.onChange(update);

        return {
            setOpacity : function(o) {
                opacity = o;
                if(mapLayer) { 
                    mapLayer.setOpacity(opacity); 
                }
            },
        };
    })();
    
    var vectorLayer = (function() {
	var markers = [];

	var createNewResourceMarker = (function(resourceData) {
	    var marker = L.marker([resourceData.lat, resourceData.lng], {
		draggable: false,
		clickable: true  
	    }).addTo(map);

	    var popupHtml = generatePopupHtml($("#popup-template").clone(), resourceData);
	    markers.push(marker);
	    marker.bindPopup(popupHtml);
	});

	var generatePopupHtml = (function(template, resourceData) {
	    var phoneString = _.reduce(resourceData.phones, function(s, v){ return s + "<br/>" + v });
	    var emailsString = _.reduce(resourceData.emails, function(s, v){ return s + "<br/>" + v });

	    var startReady = requestModel.getStartAddr().replace(" ", "+");
	    var destReady = resourceData.address.replace(" ", "+"); 
	    var hrefString = "https://maps.google.com/maps?saddr=" + startReady + "&daddr=" + destReady;

	    template.find("#resource-name").html(resourceData.name);
	    template.find("#resource-emails").html(emailsString);
	    template.find("#resource-phones").html(phoneString);
	    template.find("#resource-address").html(resourceData.address);
	    template.find("#resource-address").attr("href", hrefString);

	    return template.html();
	});


        var update = function() {
            if (markers) {
                _.each(markers,function(m) { 
                    map.removeLayer(m);
                });
                markers  = [];
            }

            var modes 	   = requestModel.getModesString();
            if(modes != "" ) {
                var latLng = requestModel.getLatLng();
                var time = requestModel.getTime();
                var duration = requestModel.getDuration();
                var direction = requestModel.getDirection();
                var schedule = requestModel.getSchedule();

                $.ajax({
                    url: GTT.Constants.BASE_URL + '/enterreturn/json',
                    dataType: "json",
                    data: { 
                        latitude: latLng.lat,
                        longitude: latLng.lng,
                        time: time,
                        duration: duration,
                        modes: modes,
                        schedule: schedule,
                        direction: direction,
			cols: 200,
			rows: 200                        
                    },
                    success: function(data) {
                        if (markers) {
                            _.each(markers,function(m) { 
                                map.removeLayer(m);
                            });
                            markers  = [];
                        }
                        _.each(data.resources,function(r) {
                            createNewResourceMarker(r);
                        })
                   }
                })
            }
        }

        requestModel.onChange(update);

        return { update : update };
    })();
                          
    var startMarker = (function() {
        var lat = GTT.Constants.START_LAT;
        var lng = GTT.Constants.START_LNG;

        var marker = L.marker([lat,lng], {
            draggable: true 
        }).addTo(map);
        
        marker.on('dragend', function(e) { 
            lat = marker.getLatLng().lat;
            lng = marker.getLatLng().lng;
            requestModel.setLatLng(lat,lng);
        } );

        return {
            getMarker : function() { return marker; },
            getLat : function() { return lat; },
            getLng : function() { return lng; },
            setLatLng : function(newLat,newLng) {
                lat = newLat;
                lng = newLng;
                marker.setLatLng(new L.LatLng(lat, lng));
                requestModel.setLatLng(lat,lng);
            }
        }
    })();

    var resourceMarkers = (function() {
		var markers = [];

		var createNewResourceMarker = (function(resourceData) {
		    var marker = L.marker([resourceData.lat, resourceData.lng], {
		        draggable: false,
		        clickable: true  
		    }).addTo(map);

			var popupHtml = generatePopupHtml($("#popup-template").clone(), resourceData);
			markers.push(marker);
			marker.bindPopup(popupHtml);
		});

		var removeAllMarkers = (function() {
			
		});

		var generatePopupHtml = (function(template, resourceData) {
			var phoneString = _.reduce(resourceData.phones, function(s, v){ return s + "<br/>" + v });
			var emailsString = _.reduce(resourceData.emails, function(s, v){ return s + "<br/>" + v });

			var startReady = requestModel.getStartAddr().replace(" ", "+");
			var destReady = resourceData.address.replace(" ", "+"); 
			var hrefString = "https://maps.google.com/maps?saddr=" + startReady + "&daddr=" + destReady;

			template.find("#resource-name").html(resourceData.name);
			template.find("#resource-emails").html(emailsString);
			template.find("#resource-phones").html(phoneString);
			template.find("#resource-address").html(resourceData.address);
			template.find("#resource-address").attr("href", hrefString);

			return template.html();
		});

        return {
            createNewResourceMarker : createNewResourceMarker
        };
    })();

    return {
        onLoadGoogleApiCallback : GTT.Geocoder.onLoadGoogleApiCallback,
        onReady : function() {
            GTT.Geocoder.setup();
            UI.wireUp(requestModel);
            UI.wireUpDynamicRendering(requestModel);
            UI.createOpacitySlider("#opacity-slider",transitShedLayer);
            UI.createDurationSlider(requestModel,vectorLayer.update);
            UI.createAddressSearch("address", function(data) {
                data = {results: data};

                if (data.results.length != 0) {
                    var lat = data.results[0].geometry.location.lat();
                    var lng = data.results[0].geometry.location.lng();
                    startMarker.setLatLng(lat,lng);
                    requestModel.setStartAddr(input.val);
                } else {
                    alert("Address not found!");
                }
            });
            requestModel.notifyChange();
        },
	resourceMarkers : resourceMarkers
    };
})();
//http://ec2-50-19-68-65.compute-1.amazonaws.com/api/enterreturn/json?latitude=39.950510086014404&longitude=-75.1640796661377&time=52426&durations=3600&modes=walking%2Cbus&schedule=weekday&direction=departing&cols=200&rows=200
// On page load
$(document).ready(function() {
    APP.onReady();
});
