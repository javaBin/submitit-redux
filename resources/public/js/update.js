"use strict";

$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    return results ? results[1] || 0 : 0;
}

function getDummySpeakerId() {
	var dummySpeakerId;
	$.ajax({
		url: "newSpeakerId",
		async: false,
		cache: false,
		success: function(data) {
			dummySpeakerId=$.parseJSON(data);
		}
	});
	return dummySpeakerId.dummyId;
}


function UpdateCtrl($scope,$http) {
	
	$scope.tagList = [];
	$scope.talk = {
		presentationType : "presentation",
		title: "",
		abstract: "",
		language: "no",
		level: "beginner",
		outline: "",
		highlight: "",
		equipment: "",
		talkTags: [],
		expectedAudience: "",
		speakers: [{
			speakerName: "",
			email: "",
			bio: "",
			picture: null,
			zipCode: "",
			givenId: null,
			dummyId: getDummySpeakerId()
		}]
	};

	$http({method: 'GET', url: "tagCollection"}).
	  		success(function(data, status, headers, config) {
	  			$scope.tagList = data;
	  			var talkid = $.urlParam("talkid");

				if (talkid != 0) {
					var jsonurl = "talkJson?talkid=" + talkid;


					$http({method: 'GET', url: jsonurl}).
				  		success(function(data, status, headers, config) {
				  			$scope.talk = data;
				  			$scope.talk.talkTags.forEach(function (tagname) {
				  				$scope.tagList.forEach(function(atag) {
				  					if (tagname == atag.value) {
				  						atag.checked=true;
				  					}
				  				});
				  			});

				  		}).
				  		error(function(data, status, headers, config) {
						    console.log("some error occured");
					  	});
				  	
				}



	  		}).
	  		error(function(data, status, headers, config) {
			    console.log("some error occured");
		  	});


	

	$scope.activeClass = function(model,value) { 
		return (value == model) ? "active" : "";
	};

	$scope.setPresentationType = function(value) {
		$scope.talk.presentationType = value;
	}

	$scope.setLanguage = function(value) {
		$scope.talk.language = value;
	}

	$scope.setLevel = function(value) {
		$scope.talk.level = value;
		console.log($scope.tagList);
	}

	


}