package mts.teta.resizer;

import mts.teta.resizer.imageprocessor.BadAttributesException;

import picocli.CommandLine;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.Resizers;
import net.coobird.thumbnailator.builders.BufferedImageBuilder;

import marvin.image.MarvinImage;
import marvinplugins.MarvinPluginCollection;

import java.io.File;
import java.io.IOException;

import java.util.concurrent.Callable;

class ConsoleAttributes {

    @CommandLine.Parameters(index = "0", paramLabel = "input-file", hidden = true)
    protected File inputFile;

    @CommandLine.Option(names = "--resize",split = " ", arity = "2",  description = "resize the image",paramLabel = "width height")
    protected int[] resizeImage = {-1, -1};

    @CommandLine.Option(names = "--quality", description = "JPEG/PNG compression level",paramLabel = "value", required = true, defaultValue = "100")
    protected int qualityImage = 100;

    @CommandLine.Option(names = "--crop", split = " ", arity = "4", description = "cut out one rectangular area of the image",paramLabel = "width height x y")
    protected int[] cropImage;

    @CommandLine.Option(names = "--blur", description = "reduce image noise detail levels", paramLabel = "{radius}", defaultValue = "0")
    protected int blurImage = 0;

    @CommandLine.Option(names = "--format", description = "the image format type",paramLabel = "\"outputFormat\"", defaultValue = "JPEG")
    protected String typeFormatImage = "JPEG";

    @CommandLine.Parameters(paramLabel = "output-file", hidden = true)
    protected File outputFile;

}

class validateAttributes {

    public void validate(int[] resizeImage, int qualityImage, int[] cropImage, int blurImage, String typeFormatImage) throws BadAttributesException {
        if (
            (resizeImage[0] != -1 && !isValidateResize(resizeImage)) ||
            !isValidateQuality(qualityImage) ||
            (cropImage != null && !isValidateCrop(cropImage)) ||
            !isValidateBlur(blurImage) ||
            !isValidateTypeFormat(typeFormatImage)
        ) {
            throw new BadAttributesException("Please check params!");
        }
    }

    public boolean isValidateResize(int[] resizeImage) {
        return (resizeImage[0] >= 0 && resizeImage[1] >= 0);
    }

    public boolean isValidateQuality(int qualityImage) {
        return (qualityImage >= 0 && qualityImage <= 100);
    }

    public boolean isValidateCrop(int[] cropImage) {
        return (cropImage[0] >= 0 && cropImage[1] >= 0 && cropImage[2] >= 0 && cropImage[3] >= 0);
    }

    public boolean isValidateBlur(int blurImage) {
        return (blurImage >= 0);
    }

    public boolean isValidateTypeFormat(String typeFormatImage) {
        return (typeFormatImage.equalsIgnoreCase("JPEG") || typeFormatImage.equalsIgnoreCase("PNG"));
    }
}

@CommandLine.Command(
        name = "resizer",
        version = "resizer 1.0.0",
        header = "resizer 1.0.0 https://github.com/Wlokas/crop-crop-mts\nAvailable formats: jpeg png",
        description = "Tool for resize, crop and blur images | MTS.Teta",
        optionListHeading = "Options Settings:\n",
        sortOptions = false,
        customSynopsis = "resizer input-file [options ...] output-file"
)
public class ResizerApp extends ConsoleAttributes implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = runConsole(args);
        System.exit(exitCode);
    }

    protected static int runConsole(String[] args) {
        return new CommandLine(new ResizerApp()).execute(args);
    }

    //Setters
    public void setInputFile(File inputImage) {
        inputFile = inputImage;
    }

    public void setOutputFile(File outputImage) {
        outputFile = outputImage;
    }

    public void setResizeWidth(int widthImage) {
        resizeImage[0] = widthImage;
    }

    public void setResizeHeight(int heightImage) {
        resizeImage[1] = heightImage;
    }

    public void setQuality(int qualityLevel) {
        qualityImage = qualityLevel;
    }

    //Getters
    public File getInputFile() {
        return inputFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public int[] getResize() {
        return resizeImage;
    }

    public int getQuality() {
        return qualityImage;
    }

    public int[] getCrop() { return cropImage; }

    public int getBlur() { return blurImage; }

    public String getTypeFormat() { return typeFormatImage; }

    @Override
    public Integer call() throws Exception {
        ImageProcessor imageProcessor = new ImageProcessor();
        imageProcessor.processImage(ImageIO.read(inputFile), this);
        return 0;
    }
}

class ImageProcessor extends validateAttributes {
    public void processImage(BufferedImage image, ResizerApp resizerApp) throws BadAttributesException, IOException {

        // Validation attributes
        validate(resizerApp.getResize(), resizerApp.getQuality(), resizerApp.getCrop(), resizerApp.getBlur(), resizerApp.getTypeFormat());

        if(resizerApp.getBlur() != 0 || resizerApp.getCrop() != null) {
            MarvinImage mImage = new MarvinImage(image);

            if (resizerApp.getBlur() != 0) {
                MarvinPluginCollection.gaussianBlur(mImage, mImage, resizerApp.getBlur());
                mImage.update();
            }

            if(resizerApp.getCrop() != null) {
                MarvinImage imageOut = new MarvinImage();
                MarvinPluginCollection.crop(mImage, imageOut,
                        resizerApp.getCrop()[2], resizerApp.getCrop()[3],
                        resizerApp.getCrop()[0], resizerApp.getCrop()[1]);
                mImage = imageOut;
                mImage.update();
            }
            image = mImage.getBufferedImage();
        }

        if(resizerApp.getResize()[0] != -1) {
            BufferedImage destImage = new BufferedImageBuilder(resizerApp.getResize()[0], resizerApp.getResize()[1]).build();
            Resizers.BILINEAR.resize(image, destImage);
            image = destImage;
        }

        Thumbnails.of(image)
                .size(image.getWidth(), image.getHeight())
                .outputFormat(resizerApp.getTypeFormat())
                .outputQuality((double) resizerApp.getQuality() / 100)
                .toFile(resizerApp.getOutputFile());
    }
}
