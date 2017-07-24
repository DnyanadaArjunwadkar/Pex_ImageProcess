follwoing steps were involved:

#1. Read the files containing urls of images.
#2. Load the images in Java and get the RGB value of every pixel in the image (This was done concurrently to maximize resources)
#3. Load the RGB value and frequncy of every pixel in a Map.
#4. Sort the map based on value of frequency. and return the 3 colors with maximum frequency
#5. Write the file in CSV format in url;color;color;color format

I implemented above approach with and without multi threading. out of 3 runs for each, multithreaded environment gave faster performance
Another approach could have been resizing the image and then processing it. but we must consider there could loss in quality of the image and time for the resize.
