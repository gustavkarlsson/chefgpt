package se.gustavkarlsson.chefgpt.auth

val registrationRules =
    listOf(
        UserRegistrationRule.name("Username must be at least 3 characters long") { name ->
            name.length >= 3
        },
        UserRegistrationRule.name("Username must start with a letter") { name ->
            name.firstOrNull()?.isLetter() ?: false
        },
        UserRegistrationRule.name("Username must only contain letters, digits, '-', and '_'") { name ->
            name.all { it.isLetterOrDigit() || it == '-' || it == '_' }
        },
        UserRegistrationRule.password("Password must be at least 8 characters") { password ->
            password.length >= 8
        },
        UserRegistrationRule.password("Password must contain only valid characters") { password ->
            password.none { it.isISOControl() } && password.all { it.isDefined() }
        },
        UserRegistrationRule.password(
            "Password must contain at least three of the following: lower-case letter, upper-case letter, number, special character",
        ) { password ->
            // TODO Set a better algorithm for complexity
            val criteriaCount =
                listOf<Char.() -> Boolean>(
                    { isLowerCase() },
                    { isUpperCase() },
                    { isDigit() },
                    { !isLetterOrDigit() },
                ).count { isCharCriteria ->
                    password.any(isCharCriteria)
                }
            criteriaCount >= 3
        },
    )
