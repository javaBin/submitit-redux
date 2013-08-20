$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    return results ? results[1] || 0 : 0;
}

function TalkDetailCtrl($scope,$http) {
	$scope.showMain = true;
	$scope.showError = false;
	var givenId = $.urlParam("talkid");
	var jsonurl = "talkJson?talkid=" + givenId;

	$scope.talkData = {}

	$http({method: 'GET', url: jsonurl}).
  		success(function(data, status, headers, config) {
  			$scope.talkData = data;
  		}).
  		error(function(data, status, headers, config) {		    
  			$scope.showMain = false;
			$scope.showError = true;
	  	});

	
	$scope.formatLanguage = function() {
		if ($scope.talkData.language === "no") {
			return "Norwegian";
		} else {
			return "English";
		}
	};

	$scope.formatTags = function() {
		var res;
		if ($scope.talkData.talkTags && $scope.talkData.talkTags.length > 0) {
			res = $scope.talkData.talkTags.reduce(function(previousValue, currentValue, index, array){
				return previousValue + ", " + currentValue;
			});
		} else {
			res = "No tags";
		}
		return res;
	};

	$scope.readpic = function(picsource) {
		if (picsource) {
			return "speakerPhoto?photoid=" + picsource;
		}
		
	};

}