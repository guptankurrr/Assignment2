 
pipeline {
    agent any

    environment {
        SONARQUBE = 'SonarQube'   
        DEP = 'C:\\Program Files\\dependency-check\\dependency-check'
        NVD_API_KEY = 'be7f8997-aef6-4e00-9c65-3176a78afabd' 
        RECIPIENTS = 'guptankurrr@gmail.com'
    }
 
    stages {
          stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/guptankurrr/jenkins-example.git'
  
            }
        }
 

 stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv('SonarQube') {
                        
                        bat 'sonar-scanner -Dsonar.projectKey=jenkins-example -Dsonar.sources=. -Dsonar.host.url=http://localhost:9000 -Dsonar.login=sqp_207217fd51a3f96e9e497ba06bc54fba92ac9dee   -Dsonar.java.binaries=target/classes'
                    }
                }
            }
        }
              stage('Quality Gate') {
            steps {
                script {
                    
                    def qg = waitForQualityGate()  
                    if (qg.status != 'OK') {
                        error "SonarQube quality gate failed: ${qg.status}"   
                    }
                }
            }
        }
         stage('Build with JaCoCo') {
            steps {
             
                    
                    echo 'Building the application with JaCoCo coverage...'
                     bat 'mvn clean install -DskipTests=true'  
            
            }
        }
        
           stage('Run Tests with JaCoCo') {
            steps {
                script {
                  
                    echo 'Running tests with JaCoCo...'
                    bat 'mvn clean test'   
                }
            }
        }
    
         stage('Publish JaCoCo Coverage Report') {
            steps {
                script {
                    
                    echo 'Publishing JaCoCo coverage report...'
                    jacoco(execPattern: '**/target/*.exec', classPattern: '**/target/classes', sourcePattern: '**/src/main/java')
                }
            }
        }
     
           stage('Run Cyclomatic Complexity Analysis') {
            steps {
                script {
                   
                    bat 'lizard . --output=complexity_report.txt'
                    
                  
                    bat 'type complexity_report.txt'
                }
            }
        }
        
 
 stage('Run Dependency-Check') {
            steps {
                script {
                    
                    bat """
                   
                         "${DEP}/bin/dependency-check.bat" --scan . --out dependency-check-report --format HTML --nvdApiKey %NVD_API_KEY%  
                    """
                }
            }
        }
      
        stage('Archive Reports') {
            steps {
               
                archiveArtifacts artifacts: 'dependency-check-report/*.html', allowEmptyArchive: true
            }
        }

        stage('Publish Dependency-Check Results') {
            steps {
              
                dependencyCheckPublisher pattern: 'dependency-check-report/*.html'
            }
        }



    }
    post {
        success {
           
            emailext(
                subject: "Build Successful",
                body: "The build was successful}",
                to: "${env.RECIPIENTS}"
            )
        }
        failure {
          
            emailext(
                subject: "Build Failed",
                body: "The build failed",
                to: "${env.RECIPIENTS}"
            )
        }
       }
    }
