package science.icebreaker.wiki;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.springframework.web.bind.annotation.*;

import science.icebreaker.exceptions.EntityNotFoundException;
import science.icebreaker.validation.exception.IllegalRequestParameterException;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class WikiController {

    private final WikiPageRepository wikiPageRepository;

    public WikiController(WikiPageRepository wikiPageRepository) {
        this.wikiPageRepository = wikiPageRepository;
    }

    @PostMapping("/wiki")
    @ApiOperation("Add a new wiki page.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "ID of the new device/wiki page"),
            @ApiResponse(code = 400, message = "Parameters not valid")})
    public int addWikiPage(@RequestBody @Valid WikiPage wikiPage) throws IllegalRequestParameterException {
        // prevent modification of existing wiki pages
        if (wikiPage.getId() != 0) {
            throw new IllegalRequestParameterException("id", "ID must not been set.");
        }
        WikiPage res = wikiPageRepository.save(wikiPage);
        return res.getId();
    }

    @GetMapping("/wiki")
    @ApiOperation("Get all wiki pages by type.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "a list of devices/wiki pages"),
            @ApiResponse(code = 400, message = "Parameter not valid")})
    public List<WikiPage> getWikiPages(@RequestParam WikiPage.PageType type) {
        return wikiPageRepository.findAllByType(type);
    }

    @GetMapping("/wiki/{id}")
    @ApiOperation("Get a wiki page by ID")
    @ApiResponses(value ={
        @ApiResponse(code = 200, message = "The wiki page entry"),
        @ApiResponse(code = 404, message = "Wiki page entry not found")
    })
    public WikiPage getWikiPage(@PathVariable Integer id) throws EntityNotFoundException {
        Optional<WikiPage> wikiPage = wikiPageRepository.findById(id);
        if(wikiPage.isPresent()) return wikiPage.get();
        else throw new EntityNotFoundException("Could not find a wikipage corresponding to the given ID");
    }
}



