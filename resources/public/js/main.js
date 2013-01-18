"use strict";

var SpeakerModel = Backbone.Model.extend({

});

var SpeakerCollection = Backbone.Collection.extend({

});

var SpeakerView = Backbone.View.extend({
	initialize: function (attrs) {
		this.template = _.template(attrs.template);
	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
	}
})

var SubmitFormModel = Backbone.Model.extend({
	initialize: function (attrs) {	
	}
});

var SubmitFormView = Backbone.View.extend({
	events: {
            "click #submitButton": "submitClicked"
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

		this.model.url="/addTalk";

		this.model.save();


		return false;
	}

});

$(function() {
	var speakerCollection = new SpeakerCollection;
	speakerCollection.add({});

	var submitFormModel = new SubmitFormModel({
		speakers: speakerCollection
	});

	var fv=new SubmitFormView({
		model: submitFormModel,
		el: $("#main"),
		template: $("#submitFormTemplate").html(),
		speakerTemplate: $("#speakerTemplate").html()
	})
	fv.render();
});