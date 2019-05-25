# MyOCR
Githubbed one of my android projects in order to get more familiar with GitHub.


# App Description
The app allows to load image from gallery or picture taken directly from camera and OCR text from whole image or part of it. Additionally there are some more options, for example saving OCR'ed text to file on your device or to google it.
If you hold Menu button, additional button '0-1' shows up, allowing to manually set up and execute binarization of the image. From there you can choose binarization options such as type: global or segmented. Global binarization means that you can specify a threshold for binarization for better text quality. If you'll set the threshold to 0, it will be automatically calculated with OTSU algorithm. In segmented binarization, however, the threshold is always automatically computed with OTSU method and you are allowed to set up amount of segments from 1 to 10. Segment size width(w) and height(h) is set calculated from whole image size(WxH): w = W/#segments, h=H/#segments (segments in last row/column can have smaller size obviously, depending if there's non-zero division remainder)

# Known bugs
I'm aware that the app crashes on my device (1gb ram) when too many operations are executed in one session. That's probably due to out of memory issue. This doesn't happen on the emulator though.
Another issue is that after binarization of the image, the OCR goes crazy and either doesn't recognize text or recognizes it super badly, therefore I'd recommend to OCR color images/photos. Consider binarization a bonus feature only, for the time.
