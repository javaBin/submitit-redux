"use strict";

$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    return results ? results[1] || 0 : 0;
}


var ErrorPageModel = Backbone.Model.extend({

});

var ErrorPageView = Backbone.View.extend({
	initialize: function (attrs) {
		this.template = _.template(attrs.template);
	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
	}
});

var SpeakerModel = Backbone.Model.extend({

});


var SpeakerCollection = Backbone.Collection.extend({
	model: SpeakerModel
});

var SpeakerView = Backbone.View.extend({
	events: {
            "change .speakerInput" : "speakerInputChanged",
            "click #addPicBtn" : "addSpeakerPicture"
    },

	initialize: function (attrs) {
		this.template = _.template(attrs.template);
	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
	},

	speakerInputChanged: function() {		
		var self = this;
		self.model.set({
			speakerName: self.$("#speakerNameInput").val(),
			email: self.$("#emailInput").val(),
			bio: self.$("#speakerBioInput").val(), 
			zipCode: self.$("#speakerZipCodeInput").val()
		});
	},

	addSpeakerPicture: function() {
		console.log("Submitting");

		this.$('newAddPic').live('submit', function(){
      		$.post($(this).attr('action'), $(this).serialize(), function(response){
            // do something here on success
	      },'json');
    	  return false;
   		});

		return false;
	}


});

var ResultModel = Backbone.Model.extend({
	
});

var ResultView = Backbone.View.extend({
	initialize: function (attrs) {
		this.template = _.template(attrs.template);
	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
	}
});

var TagModel = Backbone.Model.extend({

});

var TagCollection = Backbone.Collection.extend({
	model: TagModel,

	url: "tagCollection",

	setupChecked : function (talkTags) {
		this.each(function(tagModel) {
			var toCheck = (talkTags.indexOf(tagModel.get("value")) !== -1);
			tagModel.set({
				checked: toCheck
			},{silent:true});
		});
	}
});

var TagView  = Backbone.View.extend({
	initialize: function (attrs) {
		this.template = _.template(attrs.template);
	},

	render: function() {
		this.$el.append(this.template(this.model.toJSON()));
	}
});

var SubmitFormModel = Backbone.Model.extend({
	

});


var CaptchaModel = Backbone.Model.extend({
	url: "loadCaptcha"
});

var CaptchaView  = Backbone.View.extend({

	events: {
		"change #captchaInput": "captchaChanged"
	},

	initialize: function (attrs) {
		this.template = _.template(attrs.template);
	},

	render: function() {
		this.$el.append(this.template({}));
	},

	captchaChanged: function() {
		var self = this;
		self.model.set({
			userRespone: self.$("#captchaInput").val()
		});
	}
});

var DummySpeakerIdModel = Backbone.Model.extend({
	url: "newSpeakerId"
});


var SubmitFormView = Backbone.View.extend({
	events: {
            "click #submitButton": "submitClicked",
            "change .talkInput" : "inputChanged",
            "click .btnGroupInput" : "btnGroupChanged",
            "click .tagCheckbox" : "tagCheckboxChanged",
            "click #addSpeaker" : "addSpeaker"
    },

	initialize: function (attrs) {
		this.template = _.template(attrs.template);
		this.speakerTemplateText = attrs.speakerTemplate;
		this.resultTemplateText = attrs.resultTemplate;
		this.tagTemplate = attrs.tagTemplate;
		this.tagCollection = attrs.tagCollection;
		this.errorPageTemplate = attrs.errorPageTemplate;
		this.captchaTemplate = attrs.captchaTemplate;
		this.captchaModel = attrs.captchaModel;
	},

	renderCaptcha: function() {
		var captchaView = new CaptchaView({
			template: this.captchaTemplate,
			model: this.captchaModel
		});

		captchaView.render();

		this.$("#captchaPart").html(captchaView.el);

	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
		var speakerDom = this.$("#speakerList");
		var self = this;
		this.model.get("speakers").each(function(speakerModel) {
			var speakerView = new SpeakerView({
				model: speakerModel,
				template: self.speakerTemplateText
			});
			speakerView.render();
			speakerDom.append(speakerView.el);

		});

		var tagDom = this.$("#tagBoxes");

		this.tagCollection.each(function(tagModel) {
			var tagView = new TagView({
				model: tagModel,
				template: self.tagTemplate
			});
			tagView.$el = tagDom;
			tagView.render();
		});

		this.renderCaptcha();

	},
	

	reloadCaptchaButton: function() {
		this.captchaModel.fetch({async: false, cache: false});
		this.renderCaptcha();
	},

	submitClicked: function() {
		var submitBtn = this.$("#submitButton");		
		submitBtn.button('loading');
		this.$("#captchaError").addClass("hideit");
		var myForm = this.$('#submitForm');
		var valid = myForm[0].checkValidity();
		if (!valid) {
			submitBtn.button('reset');
			return true;
		}

		this.model.url="addTalk";

		var self = this;

		var talkDetail = window.location.origin + "/talkDetail?talkid=";

		this.model.set({
			captchaFact: self.captchaModel.get("fact"),
			captchaAnswer: self.captchaModel.get("userRespone")
		},{silent:true});


		this.model.save({}, {success: function(model, response){
			if (response.captchaError) {
				self.$("#captchaError").removeClass("hideit");
				submitBtn.button('reset');
			}
			else if (response.errormessage) {
				var errorPageModel = new ErrorPageModel({
					errormessage: response.errormessage
				});
				var errorPageView = new ErrorPageView({
					model: errorPageModel,
					template: self.errorPageTemplate
				});
				errorPageView.render();
				self.$el.html(errorPageView.el);
			} else {
				var resultModel = resultModel = new ResultModel({
					address: response.addr,
					retError: response.retError
				});
				var resultView = new ResultView({
					model: resultModel,
					template: self.resultTemplateText
				});
				resultView.render();
				self.$el.html(resultView.el);
			}
		}});



		return false;
	},

	inputChanged: function(e) {
		var self = this;


		this.model.set({
			title: self.$("#titleInput").val(),
			abstract: self.$("#abstractInput").val(),
			outline: self.$("#outlineInput").val(),
			highlight: self.$("#highlightInput").val(),
			equipment: self.$("#equipmentInput").val(),
			expectedAudience: self.$("#expectedAudienceInput").val()			
		});
	},

	btnGroupChanged: function(e) {
		var grp =  e.target.parentElement.id;
		var val = $(e.target).val();

		if (grp === "presentationTypeGrp") {
			this.model.set({presentationType : val});
		} else 
		if (grp === "languageGrp") {
			this.model.set({language : val});
		} else 
		if (grp === "levelGrp") {
			this.model.set({level : val});
		}
	},

	tagCheckboxChanged: function(e) {
		var talkList = this.model.get("talkTags");

		if (e.currentTarget.checked) {
			talkList.push(e.currentTarget.value);
		} else {
			talkList.splice(talkList.indexOf(e.currentTarget.value), 1);
		}
		this.model.set({
			talkTags: talkList
		});
	},

	addSpeaker: function() {
		var dummySpeakerIdModel=new DummySpeakerIdModel;
		dummySpeakerIdModel.fetch({
			async : false,
			cache: false,
		});

		var newSpeaker = new SpeakerModel({
				speakerName: "",
				email: "",
				bio: "",
				picture: null,
				zipCode: "",
				givenId: null,
				dummyId: dummySpeakerIdModel.get("dummyId")

		});
		this.model.get("speakers").add(newSpeaker);
		var speakerDom = this.$("#speakerList");
		speakerDom.html("");
		var self = this;
		this.model.get("speakers").each(function(speakerModel) {
			var speakerView = new SpeakerView({
				model: speakerModel,
				template: self.speakerTemplateText
			});
			speakerView.render();
			speakerDom.append(speakerView.el);

		});
	}



	
});

$(function() {	
	var givenId = $.urlParam("talkid");
	var submitFormModel;
	if (givenId === 0) {
		var dummySpeakerIdModel=new DummySpeakerIdModel;
		dummySpeakerIdModel.fetch({
			async : false,
			cache: false,
		});

		submitFormModel = new SubmitFormModel({
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
			speakers: new SpeakerCollection({
				speakerName: "",
				email: "",
				bio: "",
				picture: null,
				zipCode: "",
				givenId: null,
				dummyId: dummySpeakerIdModel.get("dummyId")
			})
		});
	} else {
		submitFormModel = new SubmitFormModel;
		submitFormModel.fetch({
			async : false,
			cache: false,
			url : "talkJson?talkid=" + givenId 
		});
		var speakArr = submitFormModel.get("speakers");
		var speakColl = new SpeakerCollection(speakArr);
		submitFormModel.set({
			speakers: speakColl
		}, {silent: true});
	}

	
	var tagCollection = new TagCollection;
	tagCollection.fetch({
		async: false,
		cache: false
	});
	
	tagCollection.setupChecked(submitFormModel.get("talkTags"));

	var captchaModel = new CaptchaModel;

	captchaModel.fetch({async: false, cache: false});

	var fv=new SubmitFormView({
		model: submitFormModel,
		tagCollection: tagCollection,
		captchaModel: captchaModel,
		el: $("#main"),
		template: $("#submitFormTemplate").html(),
		speakerTemplate: $("#speakerTemplate").html(),
		resultTemplate: $("#resultTemplate").html(),
		tagTemplate: $("#tagTemplate").html(),
		errorPageTemplate: $("#errorPageTemplate").html(),
		captchaTemplate: $("#captchaTemplate").html()
	})
	fv.render();
});