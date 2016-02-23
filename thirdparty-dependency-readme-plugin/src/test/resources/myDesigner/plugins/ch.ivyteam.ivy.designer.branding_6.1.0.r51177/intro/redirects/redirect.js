function redirectTo(page) {
	window.location.href = getDesignerRootFolder() + page;
}

function getDesignerRootFolder() {
	// file:///C:/path/to/ivy/root/plugins/ch.ivyteam.ivy.designer.branding/intro/redirects/readme.html
	var path = window.location.href;
	var i = path.indexOf("plugins/ch.ivyteam.ivy.designer.branding")
	return path.substring(0, i);
}
