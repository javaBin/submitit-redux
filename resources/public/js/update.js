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
    return captchaFact.fact;
}


function UpdateCtrl($scope,$http) {
    $scope.showMain = true;
    $scope.showResult = false;
    $scope.showResultSuccess = false;
    $scope.showResutFailure = false;
    $scope.showCapthcaError = false;
    $scope.showFailure = false;
    $scope.failureError = false;
    $scope.talkAddress = false;
    $scope.needPassword = false;

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
            password: "",
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

        $http({method: 'GET', url: "needPassword"}).
        success(function(data, status, headers, config) {
            $scope.needPassword = data.needPassword;
        }).
        error(function(data, status, headers, config) {
            console.log("Error fetching password neeeded");
        }); 

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
                            console.log("Could not load talk");
                            $scope.showMain = false;
                            $scope.failureError = "Could not find a talk with this id"
                            $scope.showFailure = true;
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

    $scope.addASpeaker = function() {
        $scope.talk.speakers.push({
                speakerName: "",
                email: "",
                bio: "",
                picture: null,
                zipCode: "",
                givenId: null,
                dummyId: getDummySpeakerId()
            });
    }

    
    $scope.talkSubmit = function(value) {
        $scope.showCapthcaError = false;
        var submitBtn = $("#submitButton");     
        submitBtn.button('loading');

        var myForm = $('#submitForm');
        var valid = myForm[0].checkValidity();
        if (!valid) {
            submitBtn.button('reset');
            return true;
        }
        

        var talkTags = [];

        $scope.tagList.forEach(function(atag) {
            if (atag.checked) {
                talkTags.push(atag.value);
            }
        });
        $scope.talk.talkTags = talkTags;


        $http({method: 'POST', url: "addTalk", data: $scope.talk}).
                        success(function(data, status, headers, config) {
                            submitBtn.button('reset');
                            if (data.captchaError) {
                                $scope.showCapthcaError = true;                             
                            } else if (data.errormessage) {
                                $scope.showMain = false;
                                $scope.showFailure = true;
                                $scope.failureError = data.errormessage;
                            } else {
                                $scope.showMain = false;
                                $scope.showResult = true;
                                $scope.talkAddress = data.addr;
                                if (data.retError) {
                                  $scope.showResultSuccess = false;
                                  $scope.showResutFailure = true;
                                } else {
                                    $scope.showResultSuccess = true;
                                    $scope.showResutFailure = false;
                                }
                            }
                            
                        }).
                        error(function(data, status, headers, config) {
                            console.log("some error occured");
                        });
        return false;
    }

    $scope.picurl = function(email) {
        if (email) {
            var res =  get_gravatar(email);
            return res;
        }
        
    };  


}