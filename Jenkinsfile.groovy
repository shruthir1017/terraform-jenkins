pipeline {
    agent any

    parameters {
        booleanParam(name: 'autoApprove', defaultValue: false, description: 'Automatically run apply after generating plan?')
        choice(name: 'action', choices: ['apply', 'destroy'], description: 'Select the action to perform')
    }

    environment {
        AWS_ACCESS_KEY_ID     = credentials('aws-access-key-id')
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
        AWS_DEFAULT_REGION    = 'us-east-1'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/shruthir1017/terraform-jenkins.git'
            }
        }

        stage('Terraform init') {
            steps {
                terraformInit()  // Using Terraform plugin's init step
            }
        }

        stage('Plan') {
            steps {
                terraformPlan(
                    additionalArgs: ['-out=tfplan']  // Optional additional arguments
                )
                script {
                    // Capture plan output for review
                    sh 'terraform show -no-color tfplan > tfplan.txt'
                }
            }
        }

        stage('Apply / Destroy') {
            steps {
                script {
                    if (params.action == 'apply') {
                        if (!params.autoApprove) {
                            def plan = readFile 'tfplan.txt'
                            input message: "Do you want to apply the plan?",
                            parameters: [text(name: 'Plan', description: 'Please review the plan', defaultValue: plan)]
                        }

                        terraformApply(
                            input: false,      // Disable interactive input
                            plan: 'tfplan'     // Specify the plan to apply
                        )
                    } else if (params.action == 'destroy') {
                        terraformDestroy(
                            autoApprove: true  // Automatically approve destroy action
                        )
                    } else {
                        error "Invalid action selected. Please choose either 'apply' or 'destroy'."
                    }
                }
            }
        }
    }
}