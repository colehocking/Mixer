# Mixer
## Overview
This application uses the microphone on an android device to gather musical information and relay them to the user. When a song is played,
it will display the tempo/BPM, the notes being played, and a graphical display, which also responds to the music. The settings Menu allows
the user three unique interfaces:
1) A Google music search window, which will start listening for music as soon as the interface appears, and identifies the title and artist
2) A settings menu which allows the user to customize listening parameters.
3) A tap tempo interface that also identifies tempo, but using the user input from tapping the screen.

### Walkthrough

When the app is opened, it will immediately begin listening for sound. It will calculate the tempo of the audio input and the 
notes that are being played, in real time, and display them to the user. The app is not listening exclusively for musical tracks
either. The user can sing, hum, or simply talk into the microphone. It will analyze any microphone input. If no sound is detected, 
it will keep listening, the app will not shut down without input. The play button at the bottom provides a test audio sample when 
pressed to demonstrate the functionality of the app. It will either play or pause the sample, depending on if it is already playing. 
The colored lines dance and move according to the audio input, for the enjoyment of the user. They also cycle through a rainbow of colors. 
The three dots in the upper right hand corner display the options menu when selected. The user can select from three different menu options.

#### Get Song Info
Song Info allows the user to collect track information from audio input. When selected, it will immediately begin listening for music. 
If a song is discovered, the title, artist, album art, and ability to purchase this song will be displayed to the user. The user can 
return the main interface regardless of whether a song is detected or not.

#### Settings 
When the settings option is selected, the user can select the Peak Threshold level, the Silence Threshold level, and the Minimum 
InterOnset Interval level using the corresponding sliders (see Application details for more information). When the user has selected 
their preferred levels, they can select the “Done” button at the bottom, and the application will save these settings. Pressing
the “defaults” button will return these thresholds to their default levels. If the user does not wish to save the settings, they can 
select the back button in the upper lefthand corner to return to the main interface. Changes will be applied, but not saved in this 
scenario.

#### Tap Tempo
For added functionality, the application provides a tap tempo interface as well. Tapping the large, round button will register taps 
as beats and then the beats per minute (BPM) will be displayed. The button will also vibrate when a tap is registered.

### Application Details
When the application is created, a “Dispatcher” object is created. Dispatcher is a class that belongs to the TarsosDSP.jar library.
The Tarsos library (https://github.com/JorenSix/TarsosDSP) provides three objects to assist this application: a dispatcher, a, pitch 
handler, and an onset handler. The dispatcher collects audio input data through Android’s AudioRecord class. Once the dispatcher 
has acquired an audio input, the onset and pitch handler objects can be used. The onset handler provides a method to call other functions 
when a musical onset is detected. A musical onset is a cluster of sound that belongs together. Within a given decibel threshold 
(which the user can modify in settings), it will detect a peak (attack) and tail (silence/decay) of a given onset, using a combination 
of decibel and frequency information. If the peak of the onset is detected, the “handleOnset” method is called. I used a 
“BpmCalculator” object inside the Tarsos handleOnset method to record each onset peak, and provide the number of “beats” per minute (BPM).
The pitch handler provides the pitch in Hertz, as it is detected from the microphone. From the hertz calculation in the pitch handler 
method, a translation from hertz output pitch data to western musical notation is provided. If the pitch is within a relatively close range 
(TarsosDSP rounds the pitch frequency for ease of use) of 12 notes in 8 different octaves, the note is displayed, otherwise, no note is 
displayed.The graphical display responds to user input by drawing the byte buffer array data input (from the microphone) onto a canvas in 
real time. Every time a new byte array is acquired, the canvas’ “onDraw” method is called. The user is able to change three of the 
listening settings: peak threshold, silence threshold, and minimum interonset interval. Peak threshold defines the decibel threshold 
at which the onset is considered a peak. The default is 0.5dB and the selector range is from 0.0dB – 1dB. The silence threshold defines 
the decibel range at which no onset can be detected. The default is -70dB, and the range is between -140dB to 0dB. The minimum interonset 
interval defines the interval in seconds, between when peaks can be considered onsets. If 2 peaks are detected in the same interval, the 
second is not considered. The default is 0.004 seconds and it ranges from 0 – 0.01 seconds. The user can save their threshold preferences, 
which is done using Android SharedPreferences. A text popup (Toast) will appear briefly when the preferences are saved. The tap tempo 
interface also uses the BPMCalculator class, but instead of audio onsets, the 'clicks' of the user are registered in the same way onset 
peaks are registered to the BPM calculator.

When the “Get Song Info” option is selected, an intent is called to the Google Now Quick Search box, specifically the 'music search' 
portion. The quick search immediately begins listening for music and fingerprinting the audio input with its database, so that no other
use action is required. Google provides this service freely, but to my knowledge, restricts manipulations of ID3 as variables within the
application. Manipulating ID3 tag information (title/artist/etc.) typically requires paid subscriptions to music fingerprinting databases.

### Future Considerations
I want to discover the overall musical key of a song/track using microphone sensor contextual information. I presumed that if I could 
acquire notes, that key was within the range of possiblity, but in order to detect the pitch of an auditory input, the pitchHandler 
processes the input data using Fast Fourier Transformations (FFT). This auditory data is only capable of monophonic pitch detection. 
If a chord is played composed of multiple notes, only the dominant note will be detected, which is not always the root. The key is 
determined from the root/tonic note, and many chords are dominated by fifths, thirds, etc. Although the application detects prevalent 
pitches accurately, and can display notes that are in fact being played, it is incapable of detecting multiple pitches at the same time; 
and makes key detection extremely difficult. I attempted to gather detected notes into an array and determine the key from the
collection, but when I used music samples of which the key was known, the application-determined key was incorrect. The chords played in 
detected samples, however, did contain the prevalent pitches roughly 85% of the time. This is still a useful functionality to have in
the application, as many songs that play the same notes are not in the same key. The can be used for music mixing the portions of 
different songs that contain the same notes.
