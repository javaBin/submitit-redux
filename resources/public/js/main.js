"use strict";

$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    return results ? results[1] || 0 : 0;
}


var SpeakerModel = Backbone.Model.extend({

});


var SpeakerCollection = Backbone.Collection.extend({
	model: SpeakerModel
});

var SpeakerView = Backbone.View.extend({
	events: {
            "change .speakerInput" : "speakerInputChanged",
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
			bio: self.$("#speakerBioInput").val()
		});
	}
})

var SubmitFormModel = Backbone.Model.extend({
	initialize: function (attrs) {	
	}
	

});

var SubmitFormView = Backbone.View.extend({
	events: {
            "click #submitButton": "submitClicked",
            "change .talkInput" : "inputChanged",
            "click .talkInput" : "inputChanged"
    },

	initialize: function (attrs) {
		this.template = _.template(attrs.template);
		this.speakerTemplateText = attrs.speakerTemplate;
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
	},

	submitClicked: function() {
		var myForm = this.$('#submitForm');
		var valid = myForm[0].checkValidity();
		if (!valid) {
			return true;
		}

		this.model.url="/addTalk";

		console.log(this.model);

		this.model.save();


		return false;
	},

	inputChanged: function() {
		var self = this;

		this.model.set({
			presentationType : self.$("#presentationTypeGrp button.active").val(),
			title: self.$("#titleInput").val(),
			abstract: self.$("#abstractInput").val(),
			language: self.$("#languageGrp button.active").val(),
			level: self.$("#levelGrp button.active").val(),
			outline: self.$("#outlineInput").val(),
			highlight: self.$("#highlightInput").val(),
			equipment: self.$("#equipmentInput").val(),
			expectedAudience: self.$("#expectedAudienceInput").val()
		});
	},

	
});

$(function() {	

	var submitFormModel = new SubmitFormModel({
		presentationType : "",
		title: "",
		abstract: "",
		language: "no",
		level: "",
		outline: "",
		highlight: "",
		equipment: "",
		expectedAudience: "",
		speakers: new SpeakerCollection({
			speakerName: "",
			email: "",
			bio: ""
		})
	});
	

	var fv=new SubmitFormView({
		model: submitFormModel,
		el: $("#main"),
		template: $("#submitFormTemplate").html(),
		speakerTemplate: $("#speakerTemplate").html()
	})
	fv.render();
});