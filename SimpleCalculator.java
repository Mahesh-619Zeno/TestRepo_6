import java.util.InputMismatchException;
import java.util.Scanner;

public class SimpleCalculator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        double num1 = getValidNumber(scanner, "Enter first number: ");
        char operator = getValidOperator(scanner);
        double num2 = getValidNumber(scanner, "Enter second number: ");

        double result = performCalculation(num1, num2, operator);
        if (result != Double.MIN_VALUE) {
            System.out.printf("Result: %.2f %c %.2f = %.2f%n", num1, operator, num2, result);
        }

        scanner.close();
    }

    private static double getValidNumber(Scanner scanner, String prompt) {
        double number = 0;
        boolean valid = false;

        while (!valid) {
            try {
                System.out.print(prompt);
                number = scanner.nextDouble();
                valid = true;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                scanner.next(); // Clear the invalid input
            }
        }

        return number;
    }

    private static char getValidOperator(Scanner scanner) {
        char operator = ' ';
        boolean valid = false;

        while (!valid) {
            System.out.print("Enter operator (+, -, *, /): ");
            String input = scanner.next();
            if (input.length() == 1) {
                operator = input.charAt(0);
                if ("+-*/".indexOf(operator) != -1) {
                    valid = true;
                } else {
                    System.out.println("Invalid operator. Please enter one of (+, -, *, /).");
                }
            } else {
                System.out.println("Invalid input. Please enter a single operator.");
            }
        }

        return operator;
    }

    private static double performCalculation(double num1, double num2, char operator) {
        double result = Double.MIN_VALUE;

        switch (operator) {
            case '+':
                result = num1 + num2;
                break;
            case '-':
                result = num1 - num2;
                break;
            case '*':
                result = num1 * num2;
                break;
            case '/':
                if (num2 != 0) {
                    result = num1 / num2;
                } else {
                    System.out.println("Error: Cannot divide by zero.");
                }
                break;
            default:
                System.out.println("Invalid operator.");
        }

        return result;
    }
}
