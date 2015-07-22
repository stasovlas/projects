//path = "D:/programs/Fiji.app/book.pdf";
toSave = getArgument();
if(toSave == "") toSave = "true";
open();
//selectWindow(path);
//run("Make Binary", "method=Default background=Dark calculate black");
run("8-bit");
setBatchMode(true);
width = getWidth();
height = getHeight();
leftPages = newArray(nSlices);
rightPages = newArray(nSlices);
indL = 0;
indR = 0;

for(z = 1; z <= nSlices; z++){
	setSlice(z);
	leftDistance = 0;
	rightDistance = 0;
	leftCount = 0;
	rightCount = 0;
	leftDistances = newArray(height);
	rightDistances = newArray(height);

	//find all left and right distances
	for(y = 0; y < height; y++){
		leftDistances[y] = 0;
		rightDistances[y] = 0;
		done = false;
		for(x = 0; x < width && !done; x++){
			pixel = getPixel(x, y);
			if(pixel > 230) leftDistances[y]++;
			else done = true;
		}
		done = false;
		for(x = width - 1; x >= 0 && !done; x--){
			pixel = getPixel(x, y);
			if(pixel > 230) rightDistances[y]++;
			else done = true;
		}
	}
	
	//find left distance
	List.clear();
	for(i = 0; i < leftDistances.length; i++){
		dist = leftDistances[i];
		if(List.get(dist) == ""){
			List.set(dist, 1);	
		} else {
			List.set(dist, parseInt(List.get(dist)) + 1);
		}
	}
	for(i = 0; i < leftDistances.length; i++){
		dist = leftDistances[i];
		value = parseInt(List.get(dist));
		if(value > leftCount && dist != width) leftCount = value;
	}
	for(i = 0; i < leftDistances.length; i++){
		dist = leftDistances[i];
		if(parseInt(List.get(dist)) == leftCount) leftDistance = dist;
	}
	
	// find right distance
	List.clear();
	for(i = 0; i < rightDistances.length; i++){
		dist = rightDistances[i];
		if(List.get(dist) == ""){
			List.set(dist, 1);	
		} else {
			List.set(dist, parseInt(List.get(dist)) + 1);
		}
	}
	for(i = 0; i < rightDistances.length; i++){
		dist = rightDistances[i];
		value = parseInt(List.get(dist));
		if(value > rightCount && dist != width) rightCount = value;
	}
	for(i = 0; i < rightDistances.length; i++){
		dist = rightDistances[i];
		if(parseInt(List.get(dist)) == rightCount) rightDistance = dist;
	}

	//draw lines
	for(y = 0; y < height; y++) setPixel(leftDistance, y, 1);
	for(y = 0; y < height; y++) setPixel(width - rightDistance, y, 1);
	
	//define page
	if(leftDistance < rightDistance){
		leftPages[indL] = z;
		indL++;
	}else{
		rightPages[indR] = z;
		indR++;	
	}
	
}
writeToFile();
if(toSave == "true") saveImages();

// write to file
function writeToFile(){
	String.append("{\n\t\"left\":[\n");
	for(i = 0; i < leftPages.length; i++){
		val = leftPages[i];
		if(val != 0){
			String.append("\t\t" + val);	
			if(i < leftPages.length - 1) String.append(",\n");
		}
	}
	String.append("\t],\n\t\"right\":[\n");
	for(i = 0; i < rightPages.length; i++){
		val = rightPages[i];
		if(val != 0){
			String.append("\t\t" + val);	
			if(i < rightPages.length - 1) String.append(",\n");
		}
	}
	String.append("\t]\n}");
	path = "res.json";
	if(File.exists(path)) File.delete(path);
	File.append(String.buffer, "res.json");
}
function saveImages(){
	run("Image Sequence... ", "format=JPEG use save=" + getDirectory("imagej") + "book" + File.separator);	
}