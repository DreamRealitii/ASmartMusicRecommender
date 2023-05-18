# ASmartMusicRecommender
Recommend music based on sound analysis instead of author/genre.

# Links to Documentation:
- User - https://docs.google.com/document/d/1qDOxYS7nrBuK766jSwN308Hlj1ByDYxRzhQNhwrTU5I/edit?usp=sharing
- Developer - https://docs.google.com/document/d/1JIzWRjZ62-U5YoquJwwGO_dqOaKNg8HI2ySlvivFHEs/edit?usp=sharing

Goals:
- Analyze music using only the objective information contained in a sound file.
- Link users to matching songs.

Code Layout:
- Backend
    - Analysis - The different types of analysing each song, such as Spotify or our own analysis.
    - Algorithm - Our custom sound analysis algorithm and the math used in it.
    - Spotify - Interact with Spotify API to get their analysis of music, create playlists for user.
    - Helper - Classes that help with miscellaneous functions, like parsing Json and .mp3 files.
- Frontend
    - UI - The visual organization of the program.
    - Control - How the program responds to user input.
    - Graphics - Images and fonts used in the program.

# Information for Alpha Release: 
## Demo functionality:
- Matches Spotify songs to ~50 other Spotify songs at random.
- UI is working, but prints match results to console.
- No local sound analysis for now.

## To install:
- Install most recent available version of IntelliJ (either community or ultimate version works)
- Link: https://www.jetbrains.com/idea/download/
- Copy the link in the github repository under the "Code" button.
- Open IntelliJ and use "Get from VCS" to clone repository.
<img width="601" alt="Screen Shot 2023-05-11 at 5 39 02 PM" src="https://github.com/DreamRealitii/ASmartMusicRecommender/assets/100888811/cd0c0c67-6afb-4c55-b2a6-ab20e147dc43">

- Go to File -> Project Structure and set Java level to 19 (may also need to select/install SDK).
<img width="625" alt="Screen Shot 2023-05-11 at 5 43 59 PM" src="https://github.com/DreamRealitii/ASmartMusicRecommender/assets/100888811/6db54395-c559-474b-857c-df969564dbcf">

**Java language level should say 19**!


## To build: 
- Open Maven tab on the right 
<img width="577" alt="Screen Shot 2023-05-11 at 5 27 50 PM" src="https://github.com/DreamRealitii/ASmartMusicRecommender/assets/100888811/05cb8586-e306-4cdf-957e-9c850a16ef25">

- Run ASmartMusicRecommender/Lifecycle/compile by right clicking
<img width="420" alt="Screen Shot 2023-05-11 at 5 29 50 PM" src="https://github.com/DreamRealitii/ASmartMusicRecommender/assets/100888811/2a46176c-823a-4d1f-8978-d9929124ad2f">

## To test:
- Right click on test folder and click "Run all tests". Folder is located in ASmartMusicRecommender/src/test.
<img width="420" alt="Screen Shot 2023-05-11 at 5 30 12 PM" src="https://github.com/DreamRealitii/ASmartMusicRecommender/assets/100888811/511f14ba-96e3-4f52-87d1-7f598b296be8">

- You should expect it look similar to below:
<img width="643" alt="Screen Shot 2023-05-11 at 5 35 10 PM" src="https://github.com/DreamRealitii/ASmartMusicRecommender/assets/100888811/3dc0fe6f-58fc-486f-9057-d97be43954b3">

- Additionally, if you want to view the tests, click on the test folder shown previously and open the tests folder inside

## To run: 
- Run main method in ASmartMusicRecommender/src/src/frontend/Home.java.
- When it asks for a Spotify username type in "31gy6dsm5x3oqdl5i6djftgvycii"
- If it opens spotify in your browser the password is "password123"
- Then you copy the Spotify track ID as shown on the app.
- The results will show up in your IntelliJ console.
- The links in the console are the links that will take you to the song in Spotify.
- The numbers next to the links are the analysis results based on our sound analysis algorithm.

## Use cases:
    Right now, our primary use case is finding songs similar sounding to the song that you like.

# Information for users/developers:
To develop:
- Clone repository into the most recent available version of IntelliJ and set Java level to 19.
- Assign yourself an issue or write a new issue to work on.
- If multiple people are assigned to the same issue, communicate to prevent merge conflicts.

To install:
- Make sure you have Java installed.
- Download asmr.jar file from release.
- Double-click asmr.jar file to run program.
