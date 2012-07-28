// Javascript used for updating watcher fields after clicking on the quick watch/unwatch link.
AJS.$(document).ready(function(){
	function updateWatchers(){
		var issueId = JIRA.Issue.getIssueId();
		console.log("Issue id: "+issueId);
		if(issueId !== undefined){
			console.log("defined!");
			console.log("regex");

			AJS.$.ajax({
				url: contextPath + "/rest/watcherfield/latest/watchers?atl_token="+atl_token()+"&issueId="+issueId,
				type: "GET",
				dataType: "json",
				success: function (response) {
					console.log("success");
					var html = '';
					if(response.watchers !== undefined){
						AJS.$(response.watchers).each(function(index, value){
							html += '<span class="tinylink">';
							html += '<span id="multiuser_cf_'+value.username+'"  rel="'+value.username+'" class="user-hover">'+value.displayName+'</span>';
							html += '</span>, ';
						});
					}
					html = html.replace(/(, )$/,'');
					AJS.$(response.fieldIds).each(function(index, value){
						AJS.$('#'+value+'-field').html(html);
					});
				},
				error: function(XMLHttpRequest, textStatus, errorThrown){
					console.log('Error in REST call to get watcher: ' + XMLHttpRequest.status + ',' + XMLHttpRequest.statusText);
					console.log(XMLHttpRequest);
				}
			});
		}
	}
	JIRA.bind(JIRA.Events.INLINE_EDIT_SAVE_COMPLETE, '#watching-toggle', function (types, data, fn) {
		updateWatchers();
	});
	JIRA.bind("ajaxComplete", '#toggle-watch-issue', function (event, XMLHttpRequest, ajaxOptions) {
		var regex = new RegExp(contextPath + "/rest/api/(.+)/issues/"+JIRA.Issue.getIssueId()+"/watchers(.*)", "ig");
		if(XMLHttpRequest.status == 200 && regex.test(ajaxOptions.url)){
			updateWatchers();
		}
	});
});