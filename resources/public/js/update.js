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

function readCaptchaFact() {
	var captchaFact;
	$.ajax({
		url: "loadCaptcha",
		async: false,
		cache: false,
		success: function(data) {
			captchaFact=$.parseJSON(data);
		}
	});
	return captchaFact
}


function UpdateCtrl($scope,$http) {
	
	$scope.tagList = [];

	var talkid = $.urlParam("talkid");
	var captchaFact = readCaptchaFact();
	if (talkid === 0) {
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
			captchaFact: captchaFact,
			captchaAnswer: "",
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
	}

	$http({method: 'GET', url: "tagCollection"}).
	  		success(function(data, status, headers, config) {
	  			$scope.tagList = data;

				if (talkid != 0) {
					var jsonurl = "talkJson?talkid=" + talkid;


					$http({method: 'GET', url: jsonurl}).
				  		success(function(data, status, headers, config) {
				  			$scope.talk = data;
				  			$scope.talk.captchaFact = captchaFact;
				  			$scope.talk.captchaAnswer = "";
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