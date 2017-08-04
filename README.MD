# alexa-nespresso
alexa skill for ordering coffee from [www.nespresso.com](www.nespresso.com).

## installing

### deploying the skill
[alexa-model.json](alexa-model.json) contains the model for the skill. you will have to configure the skill based on ths model in [Developer Console](https://developer.amazon.com/edw/home.html#/skills).

### running the application
    ./gradlew clean assemble

the resulting artifact is a *.tar archive located in `build/distributions/`. you can extract it and run with the following command:

    tar xf alexa-nespresso-0.1.0-SNAPSHOT.tar
    java -jar alexa-nespresso-0.1.0-SNAPSHOT.jar

or you can use the provided [Dockerfile](Dockerfile) to build a docker image and deploy the application as a docker container like this:

    docker build -t yevmel/alexa-nespresso .
    docker run -d --name alexa-nespresso -p8080:8080 yevmel/alexa-nespresso

the application relies on two environment variables to be present: **nespresso_username** and **nespresso_password**. Those will be used to access [Developer Console](https://developer.amazon.com/edw/home.html#/skills).

## word of caution
afaik there is no documented api to interact with services provided by [www.nespresso.com](www.nespresso.com). All insight in how the interaction works are based on observations done in browser during real checkout. Changes to these processes may render the code not functioning. Use it on your own risk.